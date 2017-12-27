/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package masterThesis.drt.data;

import masterThesis.drt.run.DrtConfigGroup;
import masterThesis.drt.schedule.DrtStopTask;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.RequestImpl;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.pt.transitSchedule.TransitStopFacilityImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author michalm
 */
public class DrtRequest extends RequestImpl implements PassengerRequest {



	public enum DrtRequestStatus {
		UNPLANNED, // submitted by the CUSTOMER and received by the DISPATCHER
		PLANNED, // planned - included into one of the routes
		PICKUP, // being picked up
		RIDE, // on board
		DROPOFF, // being dropped off
		PERFORMED, // completed
		REJECTED; // rejected by the DISPATCHER
	}

	private final MobsimPassengerAgent passenger;
	private final Link fromLink;
	private final Link toLink;
	private final Id<TransitStopFacility> fromStop;
	private final Id<TransitStopFacility> toStop;
	private DrtStopTask pickupTask = null;
	private DrtStopTask dropoffTask = null;
	private final double latestArrivalTime;
	private final VrpPathWithTravelData unsharedRidePath;
	private double updateTime;
	private final double maxWaitTime;
	private double modifiableLatestStartTime;



	public DrtRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
					  double earliestStartTime, double latestStartTime, double latestArrivalTime, double submissionTime,
					  VrpPathWithTravelData unsharedRidePath) {
		super(id, 1, earliestStartTime, latestStartTime, submissionTime);
		this.passenger = passenger;
		this.fromLink = fromLink;
		this.toLink = toLink;
		this.latestArrivalTime = latestArrivalTime;
		this.unsharedRidePath = unsharedRidePath;
		this.maxWaitTime = latestStartTime -submissionTime;
		this.updateTime = submissionTime + 1;
		this.modifiableLatestStartTime = latestStartTime;
		if (passenger.getCurrentFacility() instanceof ActivityWrapperFacility){
			this.fromStop = Id.create(passenger.getCurrentFacility().getId(),TransitStopFacility.class);
		}else{
			this.fromStop = null;
		}
		if (passenger.getDestinationFacility() instanceof ActivityWrapperFacility){
			this.toStop = Id.create(passenger.getDestinationFacility().getId(), TransitStopFacility.class);
		}else{
			this.toStop = null;
		}
	}

	public Id<TransitStopFacility> getFromStop(){
		return fromStop;
	}

	public Id<TransitStopFacility> getToStop(){
		return toStop;
	}


	@Override
	public Link getFromLink() {
		return fromLink;
	}

	@Override
	public Link getToLink() {
		return toLink;
	}

	@Override
	public MobsimPassengerAgent getPassenger() {
		return passenger;
	}

	public DrtStopTask getPickupTask() {
		return pickupTask;
	}

	public void setPickupTask(DrtStopTask pickupTask) {
		this.pickupTask = pickupTask;
	}

	public DrtStopTask getDropoffTask() {
		return dropoffTask;
	}

	public void setDropoffTask(DrtStopTask dropoffTask) {
		this.dropoffTask = dropoffTask;
	}

	public double getLatestArrivalTime() {
		return latestArrivalTime;
	}

	public VrpPathWithTravelData getUnsharedRidePath() {
		return unsharedRidePath;
	}

	public DrtRequestStatus getStatus() {
		if (pickupTask == null) {
			return DrtRequestStatus.UNPLANNED;
		}

		switch (pickupTask.getStatus()) {
			case PLANNED:
				return DrtRequestStatus.PLANNED;

			case STARTED:
				return DrtRequestStatus.PICKUP;

			case PERFORMED:// continue
		}

		switch (dropoffTask.getStatus()) {
			case PLANNED:
				return DrtRequestStatus.RIDE;

			case STARTED:
				return DrtRequestStatus.DROPOFF;

			case PERFORMED:
				return DrtRequestStatus.PERFORMED;

		}

		throw new IllegalStateException("Unreachable code");
	}


	public void setUpdateTime(double time) {
		this.updateTime=time;
		this.modifiableLatestStartTime = time + maxWaitTime;
	}

	@Override
	public double getLatestStartTime() {
		return this.modifiableLatestStartTime;
	}

	public double getUpdateTime(){
		return this.updateTime;
	}

}
