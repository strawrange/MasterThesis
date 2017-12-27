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

package masterThesis.drt.vrpagent;

import com.google.inject.Inject;
import masterThesis.drt.schedule.DrtStayTask;
import masterThesis.drt.schedule.DrtStopTask;
import masterThesis.drt.schedule.DrtTask;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import masterThesis.dvrp.passenger.BusStopActivity;
import masterThesis.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.RoutingModule;

/**
 * @author michalm
 */
public class DrtActionCreator implements VrpAgentLogic.DynActionCreator {
	public static final String DRT_STAY_NAME = "DrtStay";
	public final static String DRT_STOP_NAME = "DrtBusStop";
	private final PassengerEngine passengerEngine;
	private final VrpLegs.LegCreator legCreator;

	@Inject
	public DrtActionCreator(PassengerEngine passengerEngine, VrpOptimizer optimizer, QSim qSim) {
		this.passengerEngine = passengerEngine;
		legCreator = VrpLegs.createLegWithOnlineTrackerCreator((VrpOptimizerWithOnlineTracking)optimizer,
				qSim.getSimTimer());
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, Vehicle vehicle, double now) {
		DrtTask task = (DrtTask)vehicle.getSchedule().getCurrentTask();
		switch (task.getDrtTaskType()) {
			case DRIVE:
				return legCreator.createLeg(vehicle);

			case STOP:
				DrtStopTask t = (DrtStopTask)task;
				return new BusStopActivity(passengerEngine, dynAgent, t, t.getDropoffRequests(), t.getPickupRequests(),
						DRT_STOP_NAME);

			case STAY:
				return new VrpActivity(DRT_STAY_NAME, (DrtStayTask)task);

			default:
				throw new IllegalStateException();
		}
	}
}
