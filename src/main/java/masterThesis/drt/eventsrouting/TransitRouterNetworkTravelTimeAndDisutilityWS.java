/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkTravelTimeAndDisutilityVariableWW.java
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

package masterThesis.drt.eventsrouting;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import masterThesis.drt.eventsrouting.stopstoptime.StopStopTime;
import masterThesis.drt.eventsrouting.waitstoptime.WaitTime;
import masterThesis.drt.run.DrtConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * TravelTime and TravelDisutility calculator to be used with the transit network used for transit routing.
 * This version considers waiting time at stops, and takes travel time between stops from a {@link StopStopTime} object.
 *
 * @author sergioo
 */
public class TransitRouterNetworkTravelTimeAndDisutilityWS extends TransitRouterNetworkTravelTimeAndDisutility implements TravelDisutility, AfterMobsimListener {

	private Link previousLink;
	private double previousTime;
	private double cachedLinkTime;
	private final Map<Id<Link>, double[]> linkTravelTimes = new HashMap<Id<Link>, double[]>();
	private final Map<Id<Link>, double[]> linkWaitingTimes = new HashMap<Id<Link>, double[]>();
	private final int numSlots;
	private final double timeSlot;
	private final StopStopTime stopStopTime;
	private final WaitTime waitTime;
	private final TransitRouterNetworkWW routerNetwork;
	private final double qsimStartTime;
	private final DrtTransitRouterConfig config;

	@Inject
	public TransitRouterNetworkTravelTimeAndDisutilityWS(final TransitRouterConfig transitRouterConfigGroup,TransitRouterNetworkWW routerNetwork,
                                                         WaitTime waitTime, StopStopTime stopStopTime, TravelTimeCalculatorConfigGroup tTConfigGroup,
                                                         QSimConfigGroup qSimConfigGroup, @Named(DrtConfigGroup.DRT_MODE)TransitSchedule transitSchedule) {
		this(transitRouterConfigGroup,  routerNetwork, waitTime, stopStopTime, tTConfigGroup, qSimConfigGroup.getStartTime(), qSimConfigGroup.getEndTime(), transitSchedule);
	}
	public TransitRouterNetworkTravelTimeAndDisutilityWS(final TransitRouterConfig config, TransitRouterNetworkWW routerNetwork, WaitTime waitTime, StopStopTime stopStopTime,
                                                         TravelTimeCalculatorConfigGroup tTConfigGroup, double startTime,
														 double endTime, TransitSchedule transitSchedule) {
		super(config, new PreparedTransitSchedule(transitSchedule));
		this.stopStopTime = stopStopTime;
		this.waitTime = waitTime;
		this.routerNetwork = routerNetwork;
		this.config = (DrtTransitRouterConfig) config;
		timeSlot = tTConfigGroup.getTraveltimeBinSize();
		numSlots = (int) ((endTime-startTime)/timeSlot);
		qsimStartTime = startTime;
		initiate(startTime);
	}

	private void initiate(double startTime){
		for(TransitRouterNetworkWW.TransitRouterNetworkLink link:routerNetwork.getLinks().values()) {
			if(!link.fromNode.getId().toString().endsWith("_W")) {
				double[] times = new double[numSlots];
				for(int slot = 0; slot<numSlots; slot++)
					times[slot] = stopStopTime.getStopStopTime(link.fromNode.stop.getId(), link.toNode.stop.getId(), startTime+slot*timeSlot);
				linkTravelTimes.put(link.getId(), times);
			}
			else {
				double[] times = new double[numSlots];
				for(int slot = 0; slot<numSlots; slot++)
					times[slot] = waitTime.getRouteStopWaitTime(link.fromNode.stop.getId(), startTime+slot*timeSlot);
				linkWaitingTimes.put(link.getId(), times);
			}
		}
	}
	@Override
	public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
		previousLink = link;
		previousTime = time;
		TransitRouterNetworkWW.TransitRouterNetworkLink wrapped = (TransitRouterNetworkWW.TransitRouterNetworkLink) link;
		if (!wrapped.fromNode.getId().toString().endsWith("_W"))
			//in line link
			cachedLinkTime = linkTravelTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)];
		else
			//wait link
			cachedLinkTime = linkWaitingTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)];
		return cachedLinkTime;
	}
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
		boolean cachedTravelDisutility = false;
		if(previousLink==link && previousTime==time)
			cachedTravelDisutility = true;
		TransitRouterNetworkWW.TransitRouterNetworkLink wrapped = (TransitRouterNetworkWW.TransitRouterNetworkLink) link;
		if (!wrapped.fromNode.getId().toString().endsWith("_W"))
			return -(cachedTravelDisutility?cachedLinkTime:linkTravelTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)])*this.config.getMarginalUtilityOfTravelTimeDrt_utl_s() / 3600
					- link.getLength() * (this.config.getMarginalUtilityOfTravelDistanceDrt_utl_m());
        else
			// it's a wait link
			return  -(cachedTravelDisutility?cachedLinkTime:linkWaitingTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)])*this.config.getMarginalUtilityOfWaitingDrt_utl_s();
	}
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		boolean cachedTravelDisutility = false;
		if(previousLink==link && previousTime==time)
			cachedTravelDisutility = true;
		TransitRouterNetworkWW.TransitRouterNetworkLink wrapped = (TransitRouterNetworkWW.TransitRouterNetworkLink) link;
		if (!wrapped.fromNode.getId().toString().endsWith("_W"))
			return -(cachedTravelDisutility?cachedLinkTime:linkTravelTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)])*this.config.getMarginalUtilityOfTravelTimeDrt_utl_s()
					- link.getLength() * (this.config.getMarginalUtilityOfTravelDistanceDrt_utl_m());
		else
			// it's a wait link
			return -(cachedTravelDisutility?cachedLinkTime:linkWaitingTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)])*this.config.getMarginalUtilityOfWaitingDrt_utl_s();
	}
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}

	@Override
    public double getWalkTravelDisutility(Person person, Coord coord, Coord toCoord) {
        //  getMarginalUtilityOfTravelTimeWalk INCLUDES the opportunity cost of time.  kai, dec'12
        double timeCost = Math.exp(getWalkTravelTime(person, coord, toCoord) * config.getMarginalUtilityOfTravelTimeWalk_utl_s()) - 1 ;
        // (sign: margUtl is negative; overall it should be positive because it is a cost.)

        double distanceCost = - CoordUtils.calcEuclideanDistance(coord,toCoord) *
                config.getBeelineDistanceFactor() * config.getMarginalUtilityOfTravelDistanceWalk_utl_m();
        // (sign: same as above)

        return timeCost + distanceCost ;
    }

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		initiate(qsimStartTime);
	}
}
