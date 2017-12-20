/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package masterThesis.drt.passenger;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import masterThesis.drt.data.DrtRequest;
import masterThesis.drt.optimizer.DefaultDrtOptimizerProvider;
import masterThesis.drt.passenger.events.DrtRequestSubmittedEvent;
import masterThesis.drt.run.DrtConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import masterThesis.dvrp.passenger.PassengerRequestCreator;
import masterThesis.dvrp.path.VrpPathWithTravelData;
import masterThesis.dvrp.path.VrpPaths;
import masterThesis.dvrp.router.TimeAsTravelDisutility;
import masterThesis.dvrp.run.DvrpModule;
import masterThesis.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.*;

/**
 * @author michalm
 */
public class DrtRequestCreator implements PassengerRequestCreator {
	private final DrtConfigGroup drtCfg;
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;
	private final EventsManager eventsManager;
	private final MobsimTimer timer;

	@Inject(optional = true)
	private @Named(DefaultDrtOptimizerProvider.DRT_OPTIMIZER) TravelDisutilityFactory travelDisutilityFactory;

	@Inject
	public DrtRequestCreator(DrtConfigGroup drtCfg, @Named(DvrpModule.DVRP_ROUTING) Network network,
                             @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime, QSim qSim) {
		this.drtCfg = drtCfg;
		this.travelTime = travelTime;
		this.eventsManager = qSim.getEventsManager();
		this.timer = qSim.getSimTimer();

		TravelDisutility travelDisutility = travelDisutilityFactory == null ? new TimeAsTravelDisutility(travelTime)
				: travelDisutilityFactory.createTravelDisutility(travelTime);

		PreProcessEuclidean preProcessEuclidean = new PreProcessEuclidean(travelDisutility);
		preProcessEuclidean.run(network);

		router = new FastAStarEuclideanFactory().createPathCalculator(network, travelDisutility, travelTime);
	}

	@Override
	public DrtRequest createRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
									double departureTime, double submissionTime) {
		double latestDepartureTime = timer.getTimeOfDay() + drtCfg.getMaxWaitTime();

		VrpPathWithTravelData unsharedRidePath = VrpPaths.calcAndCreatePath(fromLink, toLink, departureTime, router,
				travelTime);

		double optimisticTravelTime = unsharedRidePath.getTravelTime();
		double maxTravelTime = drtCfg.getMaxTravelTimeAlpha() * optimisticTravelTime + drtCfg.getMaxTravelTimeBeta();
		double latestArrivalTime = departureTime + maxTravelTime;
		
		double unsharedDistance = VrpPaths.calcPathDistance(unsharedRidePath);
		
		eventsManager.processEvent(new DrtRequestSubmittedEvent(timer.getTimeOfDay(), id, passenger.getId(),
				fromLink.getId(), toLink.getId(), unsharedRidePath.getTravelTime(), unsharedDistance));

		return new DrtRequest(id, passenger, fromLink, toLink, departureTime, latestDepartureTime, latestArrivalTime,
				submissionTime, unsharedRidePath);
	}
}
