/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package masterThesis.drt.optimizer;

import masterThesis.drt.data.DrtRequest;
import masterThesis.drt.schedule.DrtStopTask;
import masterThesis.drt.schedule.DrtTask;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import java.util.PriorityQueue;

/**
 * @author michalm
 */
public abstract class AbstractDrtOptimizer implements DrtOptimizer {
	private final DrtOptimizerContext optimContext;
	private final PriorityQueue<DrtRequest> unplannedRequests;
;


	private boolean requiresReoptimization = false;

	public AbstractDrtOptimizer(DrtOptimizerContext optimContext, PriorityQueue<DrtRequest> unplannedRequests) {
		this.optimContext = optimContext;
		this.unplannedRequests = unplannedRequests;
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (requiresReoptimization) {
            for (Vehicle v : optimContext.fleet.getVehicles().values()) {
                optimContext.scheduler.updateTimeline(v);
            }
        }
        scheduleUnplannedRequests();
        requiresReoptimization = false;
	}

	protected abstract void scheduleUnplannedRequests();


	@Override
	public void requestSubmitted(Request request) {
		DrtRequest drtRequest = (DrtRequest)request;
		if (drtRequest.getFromLink() == drtRequest.getToLink()) {
			// throw new IllegalArgumentException("fromLink and toLink must be different");
			Logger.getLogger(getClass()).error("fromLink and toLink must be different. Request " + request.getId()
					+ " will not be served. The agent will stay in limbo.");
			return;
		}
		unplannedRequests.add(drtRequest);
		requiresReoptimization = false;
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		optimContext.scheduler.updateBeforeNextTask(vehicle);

		Task newCurrentTask = vehicle.getSchedule().nextTask();

		if (newCurrentTask instanceof DrtStopTask){
			if (((DrtStopTask) newCurrentTask).getPickupRequests().size() != 0){
				getOptimContext().qSim.getEventsManager().processEvent(new VehicleArrivesAtFacilityEvent(getOptimContext().qSim.getSimTimer().getTimeOfDay(), Id.createVehicleId(vehicle.getId()),
						((DrtStopTask) newCurrentTask).getTransitStopFacilityId(),0));
			}
		}

		if (!requiresReoptimization && newCurrentTask != null) {// schedule != COMPLETED
			requiresReoptimization = doReoptimizeAfterNextTask((DrtTask)newCurrentTask);
		}
	}

	protected boolean doReoptimizeAfterNextTask(DrtTask newCurrentTask) {
		return false;
	}

	@Override
	public void vehicleEnteredNextLink(Vehicle vehicle, Link nextLink) {
		// optimContext.scheduler.updateTimeline(vehicle);

		// TODO we may here possibly decide whether or not to reoptimize
		// if (delays/speedups encountered) {requiresReoptimization = true;}
	}

	protected PriorityQueue<DrtRequest> getUnplannedRequests() {
		return unplannedRequests;
	}

	protected DrtOptimizerContext getOptimContext() {
		return optimContext;
	}

	public void abortUnplannedRequests(EventsManager eventsManager){
		for (DrtRequest request:getUnplannedRequests()){
			eventsManager.processEvent(new PersonStuckEvent(optimContext.qSim.getSimTimer().getTimeOfDay(),request.getPassenger().getId(),request.getFromLink().getId(),request.getPassenger().getMode()));
		}
	}

}
