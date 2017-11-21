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

package masterThesis.dvrp.examples.onetaxi;

import com.google.inject.Inject;
import masterThesis.dvrp.data.Vehicle;
import masterThesis.dvrp.passenger.PassengerEngine;
import masterThesis.dvrp.passenger.SinglePassengerDropoffActivity;
import masterThesis.dvrp.passenger.SinglePassengerPickupActivity;
import masterThesis.dvrp.schedule.DriveTask;
import masterThesis.dvrp.schedule.StayTask;
import masterThesis.dvrp.schedule.Task;
import masterThesis.dvrp.vrpagent.VrpActivity;
import masterThesis.dvrp.vrpagent.VrpAgentLogic;
import masterThesis.dvrp.vrpagent.VrpLegs;
import masterThesis.dynagent.DynAction;
import masterThesis.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;

/**
 * @author michalm
 */
public class OneTaxiActionCreator implements VrpAgentLogic.DynActionCreator {
	private final PassengerEngine passengerEngine;
	private final MobsimTimer timer;

	@Inject
	public OneTaxiActionCreator(PassengerEngine passengerEngine, QSim qSim) {
		this.passengerEngine = passengerEngine;
		this.timer = qSim.getSimTimer();
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, Vehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask();
		if (task instanceof DriveTask) {
			return VrpLegs.createLegWithOfflineTracker(vehicle, timer);
		} else if (task instanceof OneTaxiServeTask) { // PICKUP or DROPOFF
			final OneTaxiServeTask serveTask = (OneTaxiServeTask)task;

			if (serveTask.isPickup()) {
				return new SinglePassengerPickupActivity(passengerEngine, dynAgent, serveTask, serveTask.getRequest(),
						OneTaxiOptimizer.PICKUP_DURATION, "OneTaxiPickup");
			} else {
				return new SinglePassengerDropoffActivity(passengerEngine, dynAgent, serveTask, serveTask.getRequest(),
						"OneTaxiDropoff");
			}
		} else { // WAIT
			return new VrpActivity("OneTaxiStay", (StayTask)task);
		}
	}
}
