/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package masterThesis.dynagent;

import masterThesis.dvrp.vrpagent.VrpLeg;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.List;

public final class DynAgent implements MobsimDriverPassengerAgent {
	private DynAgentLogic agentLogic;

	private Id<Person> id;

	private MobsimVehicle veh;

	private EventsManager events;

	private State state;

	// =====

	private Id<Link> currentLinkId;

	// =====

	private DynLeg dynLeg;
	private DynActivity dynActivity;

	// =====

	public DynAgent(Id<Person> id, Id<Link> startLinkId, EventsManager events, DynAgentLogic agentLogic) {
		this.id = id;
		this.currentLinkId = startLinkId;
		this.agentLogic = agentLogic;
		this.events = events;

		// initial activity
		dynActivity = this.agentLogic.computeInitialActivity(this);
		state = dynActivity.getEndTime() != Time.UNDEFINED_TIME ? //
				State.ACTIVITY : State.ABORT;
	}

	private void computeNextAction(DynAction oldDynAction, double now) {
		oldDynAction.finalizeAction(now);

		state = null;// !!! this is important
		dynActivity = null;
		dynLeg = null;

		DynAction nextDynAction = agentLogic.computeNextAction(oldDynAction, now);

		if (nextDynAction instanceof DynActivity) {
			dynActivity = (DynActivity)nextDynAction;
			state = State.ACTIVITY;

			events.processEvent(new ActivityStartEvent(now, id, currentLinkId, null, dynActivity.getActivityType()));
		} else {
			dynLeg = (DynLeg)nextDynAction;
			state = State.LEG;
		}
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		events.processEvent(new ActivityEndEvent(now, id, currentLinkId, null, dynActivity.getActivityType()));
		computeNextAction(dynActivity, now);
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		events.processEvent(new PersonArrivalEvent(now, id, currentLinkId, dynLeg.getMode()));
		computeNextAction(dynLeg, now);
	}

	@Override
	public void setStateToAbort(double now) {
		this.state = State.ABORT;
	}

	public DynAgentLogic getAgentLogic() {
		return agentLogic;
	}

	public DynAction getCurrentAction() {
		switch (state) {
			case ACTIVITY:
				return dynActivity;

			case LEG:
				return dynLeg;

			default:
				throw new IllegalStateException();
		}
	}

	@Override
	public Id<Person> getId() {
		return id;
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public String getMode() {
		return (state == State.LEG) ? dynLeg.getMode() : null;
	}

	// VehicleUsingAgent
	@Override
	public final Id<Vehicle> getPlannedVehicleId() {
		Id<Vehicle> vehId = ((DriverDynLeg)dynLeg).getPlannedVehicleId();
		// according to BasicPlanAgentImpl
		return vehId != null ? vehId : Id.create(id, Vehicle.class);
	}

	// VehicleUsingAgent
	@Override
	public void setVehicle(MobsimVehicle veh) {
		this.veh = veh;
	}

	// VehicleUsingAgent
	@Override
	public MobsimVehicle getVehicle() {
		return veh;
	}

	// NetworkAgent
	@Override
	public Id<Link> getCurrentLinkId() {
		return currentLinkId;
	}

	// NetworkAgent (used only for teleportation)
	@Override
	public Id<Link> getDestinationLinkId() {
		return dynLeg.getDestinationLinkId();
	}

	// DriverAgent
	@Override
	public Id<Link> chooseNextLinkId() {
		return ((DriverDynLeg)dynLeg).getNextLinkId();
	}

	// DriverAgent
	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		((DriverDynLeg)dynLeg).movedOverNode(newLinkId);
		currentLinkId = newLinkId;
	}

	// MobsimAgent
	@Override
	public double getActivityEndTime() {
		return dynActivity.getEndTime();
	}

	// DynAgent
	public void doSimStep(double now) {
		dynActivity.doSimStep(now);
	}

	// MobsimAgent
	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		dynLeg.arrivedOnLinkByNonNetworkMode(linkId);
		currentLinkId = linkId;
	}

	// MobsimAgent
	@Override
	public Double getExpectedTravelTime() {
		return dynLeg.getExpectedTravelTime();
	}

	// MobsimAgent
	@Override
	public Double getExpectedTravelDistance() {
		return dynLeg.getExpectedTravelDistance();
	}

	// DriverAgent
	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		return chooseNextLinkId() == null;
	}

	// PTPassengerAgent
	@Override
	public boolean getEnterTransitRoute(TransitLine line, TransitRoute transitRoute, List<TransitRouteStop> stopsToCome,
			TransitVehicle transitVehicle) {
		return ((PTPassengerDynLeg)dynLeg).getEnterTransitRoute(line, transitRoute, stopsToCome, transitVehicle);
	}

	// PTPassengerAgent
	@Override
	public boolean getExitAtStop(TransitStopFacility stop) {
		return ((PTPassengerDynLeg)dynLeg).getExitAtStop(stop);
	}

	// PTPassengerAgent
	@Override
	public Id<TransitStopFacility> getDesiredAccessStopId() {
		return ((PTPassengerDynLeg)dynLeg).getDesiredAccessStopId();
	}

	// PTPassengerAgent
	@Override
	public Id<TransitStopFacility> getDesiredDestinationStopId() {
		return ((PTPassengerDynLeg)dynLeg).getDesiredDestinationStopId();
	}

	// PTPassengerAgent
	@Override
	public double getWeight() {
		return 1;
	}

	@Override
	public Facility<? extends Facility<?>> getCurrentFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented");
	}

	@Override
	public Facility<? extends Facility<?>> getDestinationFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented");
	}
}
