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

package masterThesis.drt.optimizer.insertion;

import masterThesis.drt.optimizer.DrtOptimizerContext;
import masterThesis.drt.run.DrtConfigGroup;
import masterThesis.drt.schedule.DrtStayTask;
import masterThesis.drt.schedule.DrtTask;
import org.matsim.api.core.v01.network.Link;
import masterThesis.drt.schedule.DrtTask.DrtTaskType;
import masterThesis.dvrp.data.Vehicle;
import masterThesis.dvrp.path.VrpPathWithTravelData;
import masterThesis.dvrp.path.VrpPaths;
import masterThesis.dvrp.schedule.Schedule;
import masterThesis.dvrp.schedule.Schedule.ScheduleStatus;
import masterThesis.util.distance.DistanceUtils;
import org.matsim.core.router.ArrayFastRouterDelegateFactory;
import org.matsim.core.router.FastAStarEuclidean;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.FastRouterDelegateFactory;
import org.matsim.core.router.util.ArrayRoutingNetworkFactory;
import org.matsim.core.router.util.PreProcessEuclidean;
import org.matsim.core.router.util.RoutingNetwork;

import java.util.HashSet;
import java.util.Set;

/**
 * @author michalm
 */
public class InsertionDrtOptimizerWithDepots extends InsertionDrtOptimizer {
	private final Set<Link> startLinks = new HashSet<>();
	private final FastAStarEuclidean router;

	public InsertionDrtOptimizerWithDepots(DrtOptimizerContext optimContext, DrtConfigGroup drtCfg) {
		super(optimContext, drtCfg);

		for (Vehicle v : optimContext.fleet.getVehicles().values()) {
			startLinks.add(v.getStartLink());
		}

		PreProcessEuclidean preProcessEuclidean = new PreProcessEuclidean(optimContext.travelDisutility);
		preProcessEuclidean.run(optimContext.network);

		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
		RoutingNetwork routingNetwork = new ArrayRoutingNetworkFactory()
				.createRoutingNetwork(optimContext.network);

		router = (FastAStarEuclidean) new FastAStarEuclideanFactory().createPathCalculator(routingNetwork,  optimContext.travelDisutility,
				optimContext.travelTime);
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		super.nextTask(vehicle);
		Schedule schedule = vehicle.getSchedule();

		// only active vehicles
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return;
		}

		DrtTask currentTask = (DrtTask)schedule.getCurrentTask();

		// current task is STAY
		if (currentTask != null && currentTask.getDrtTaskType() == DrtTaskType.STAY) {
			int previousTaskIdx = currentTask.getTaskIdx() - 1;

			// previous task is STOP
			if (previousTaskIdx >= 0
					&& ((DrtTask)schedule.getTasks().get(previousTaskIdx)).getDrtTaskType() == DrtTaskType.STOP) {

				Link currentLink = ((DrtStayTask)currentTask).getLink();
				Link bestStartLink = findBestStartLink(currentLink);
				if (bestStartLink != null) {
					VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(currentLink, bestStartLink,
							currentTask.getBeginTime(), router, getOptimContext().travelTime);
					getOptimContext().scheduler.relocateEmptyVehicle(vehicle, path);
				}
			}
		}
	}

	// TODO a simple straight-line search (for the time being)... MultiNodeDijkstra should be the ultimate solution
	private Link findBestStartLink(Link fromLink) {
		if (startLinks.contains(fromLink)) {
			return null;// stay where it is
		}
		Link bestLink = null;
		double bestDistance = Double.MAX_VALUE;
		for (Link l : startLinks) {
			double currentDistance = DistanceUtils.calculateSquaredDistance(fromLink.getCoord(), l.getCoord());
			if (currentDistance < bestDistance) {
				bestDistance = currentDistance;
				bestLink = l;
			}
		}

		return bestLink;
	}
}
