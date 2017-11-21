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

package masterThesis.dvrp.vrpagent;

import com.google.inject.Inject;
import masterThesis.dvrp.data.Vehicle;
import masterThesis.dvrp.path.VrpPaths;
import masterThesis.dvrp.schedule.DriveTask;
import masterThesis.dvrp.schedule.Schedule;
import masterThesis.dvrp.schedule.StayTask;
import masterThesis.dvrp.schedule.Task;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import masterThesis.dvrp.schedule.Schedule.ScheduleStatus;
import masterThesis.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The class is designed for inheritance. Overrride createLeg() and createActivity() to obtain different visualisation
 * of VRP schedules/vehicles
 * 
 * @author michalm
 */
public class VrpAgentQueryHelper implements NonPlanAgentQueryHelper {
	protected final PopulationFactory populFactory;

	@Inject
	public VrpAgentQueryHelper(PopulationFactory populFactory) {
		this.populFactory = populFactory;
	}

	@Override
	public Activity getCurrentActivity(MobsimAgent mobsimAgent) {
		Vehicle vehicle = getVehicle(mobsimAgent);
		Schedule schedule = vehicle.getSchedule();

		if (schedule.getStatus() == ScheduleStatus.STARTED) {
			Task currentTask = schedule.getCurrentTask();
			if (currentTask instanceof StayTask) {
				return createActivity((StayTask)currentTask);
			}
		}

		return null;
	}

	@Override
	public Plan getPlan(MobsimAgent mobsimAgent) {
		return new VrpSchedulePlan(getVehicle(mobsimAgent));
	}

	private List<PlanElement> initPlanElements(Vehicle vehicle) {
		List<PlanElement> planElements = new ArrayList<>();
		Schedule schedule = vehicle.getSchedule();

		if (schedule.getStatus() == ScheduleStatus.STARTED) {
			for (Task t : schedule.getTasks()) {
				if (t instanceof DriveTask) {
					planElements.add(createLeg((DriveTask)t));
				} else {

					planElements.add(createActivity((StayTask)t));
				}
			}
		}

		return planElements;
	}

	private Vehicle getVehicle(MobsimAgent mobsimAgent) {
		return ((VrpAgentLogic)((DynAgent)mobsimAgent).getAgentLogic()).getVehicle();
	}

	protected Leg createLeg(DriveTask task) {
		Leg leg = populFactory.createLeg(TransportMode.car);
		leg.setRoute(VrpPaths.createNetworkRoute(task.getPath(), populFactory.getRouteFactories()));
		return leg;
	}

	protected Activity createActivity(StayTask task) {
		Activity act = populFactory.createActivityFromLinkId("s", task.getLink().getId());
		act.setStartTime(task.getBeginTime());
		act.setEndTime(task.getEndTime());
		return act;
	}

	private final class VrpSchedulePlan implements Plan {
		private List<PlanElement> unmodifiablePlanElements;

		private VrpSchedulePlan(Vehicle vehicle) {
			unmodifiablePlanElements = Collections.unmodifiableList(initPlanElements(vehicle));
		}

		@Override
		public List<PlanElement> getPlanElements() {
			return unmodifiablePlanElements;
		}

		@Override
		public Person getPerson() {
			throw new UnsupportedOperationException();
		}

		public Double getScore() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addLeg(Leg leg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addActivity(Activity act) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setType(String type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setScore(Double score) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setPerson(Person person) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Attributes getAttributes() {
			throw new UnsupportedOperationException();
		}
	}
}
