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

/**
 * 
 */
package masterThesis.drt.closerouting;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import masterThesis.drt.run.DrtConfigGroup;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.*;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author  jbischoff
 *
 */

/**
 *
 */
public class ClosestStopBasedDrtRoutingModule implements RoutingModule {

	private final StageActivityTypes drtStageActivityType = new DrtStageActivityType();
	private final RoutingModule walkRouter;
	private final RoutingModule accessWalkRouter;
	private final Network network;
	private final Scenario scenario;

	private final String mode;
	private final Map<Id<TransitStopFacility>, TransitStopFacility> stops;
	private final DrtConfigGroup drtconfig;
	private final double walkBeelineFactor;

	/**
	 *
	 */
	@Inject
	public ClosestStopBasedDrtRoutingModule( RoutingModule walkRouter,RoutingModule accessWalkRouter,
                                             TransitSchedule transitSchedule, Scenario scenario, String mode) {
		this.walkRouter = walkRouter;
		this.accessWalkRouter = accessWalkRouter;
	    this.stops = transitSchedule.getFacilities();
		this.drtconfig = (DrtConfigGroup)scenario.getConfig().getModules().get(DrtConfigGroup.GROUP_NAME);
		this.walkBeelineFactor = scenario.getConfig().plansCalcRoute().getModeRoutingParams().get(TransportMode.access_walk)
				.getBeelineDistanceFactor();
		this.network = scenario.getNetwork();
		this.scenario = scenario;
		this.mode = mode;
;	}



	@Override
	public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime,
			Person person) {
		List<PlanElement> legList = new ArrayList<>();
		TransitStopFacility accessFacility = findAccessFacility(fromFacility);
		if (accessFacility == null) {
			if (drtconfig.isPrintDetailedWarnings()){
			Logger.getLogger(getClass()).error("No access stop found, agent will walk. Agent Id:\t" + person.getId());}
			return (walkRouter.calcRoute(fromFacility, toFacility, departureTime, person));
		}
		TransitStopFacility egressFacility = findEgressFacility(toFacility);
		if (egressFacility == null) {
			if (drtconfig.isPrintDetailedWarnings()){
			Logger.getLogger(getClass()).error("No egress stop found, agent will walk. Agent Id:\t" + person.getId());}
			return (walkRouter.calcRoute(fromFacility, toFacility, departureTime, person));
		}
		legList.addAll(accessWalkRouter.calcRoute(fromFacility, accessFacility, departureTime, person));
		Leg walkLeg = (Leg)legList.get(0);
		Activity drtInt1 = scenario.getPopulation().getFactory()
				.createActivityFromCoord(DrtStageActivityType.DRTSTAGEACTIVITY, accessFacility.getCoord());
		drtInt1.setMaximumDuration(0);
		drtInt1.setLinkId(accessFacility.getLinkId());
		drtInt1.setFacilityId(transitStopFacilityIdToActivity(accessFacility.getId()));
		legList.add(drtInt1);

		Route drtRoute = RouteUtils.createGenericRouteImpl(accessFacility.getLinkId(), egressFacility.getLinkId());
		drtRoute.setDistance(drtconfig.getEstimatedBeelineDistanceFactor()
				* CoordUtils.calcEuclideanDistance(accessFacility.getCoord(), egressFacility.getCoord()));
		drtRoute.setTravelTime(drtRoute.getDistance() / drtconfig.getEstimatedDrtSpeed());

		if (drtRoute.getStartLinkId() == drtRoute.getEndLinkId()) {
			if (drtconfig.isPrintDetailedWarnings()){
			Logger.getLogger(getClass()).error("Start and end stop are the same, agent will walk. Agent Id:\t" + person.getId());
			}
			return (walkRouter.calcRoute(fromFacility, toFacility, departureTime, person));

		}
		Leg drtLeg = PopulationUtils.createLeg(mode);
		drtLeg.setDepartureTime(departureTime + walkLeg.getTravelTime() + 1);
		drtLeg.setTravelTime(drtRoute.getTravelTime());
		drtLeg.setRoute(drtRoute);

		legList.add(drtLeg);

		Activity drtInt2 = scenario.getPopulation().getFactory()
				.createActivityFromCoord(DrtStageActivityType.DRTSTAGEACTIVITY, egressFacility.getCoord());
		drtInt2.setMaximumDuration(0);

		drtInt2.setLinkId(egressFacility.getLinkId());
		drtInt2.setFacilityId(transitStopFacilityIdToActivity(egressFacility.getId()));
		legList.add(drtInt2);
		legList.addAll(accessWalkRouter.calcRoute(egressFacility, toFacility,
				drtLeg.getDepartureTime() + drtLeg.getTravelTime() + 1, person));
		return legList;
	}

	private Id<ActivityFacility> transitStopFacilityIdToActivity(Id<TransitStopFacility> transitStopFacilityId){
		return Id.create(transitStopFacilityId.toString(),ActivityFacility.class);
	}

	/**
	 * @param fromFacility
	 * @return
	 */
	private TransitStopFacility findAccessFacility(Facility<?> fromFacility) {
		Coord fromCoord = getFacilityCoord(fromFacility);
		TransitStopFacility accessFacility = findClosestStop(fromCoord);
		
		return accessFacility;
	}

	private TransitStopFacility findEgressFacility(Facility<?> toFacility) {
		Coord toCoord = getFacilityCoord(toFacility);
		TransitStopFacility stop = findClosestStop(toCoord);
		return stop;

	}


	private TransitStopFacility findClosestStop(Coord coord) {
		TransitStopFacility bestStop = null;
		double bestDist = Double.MAX_VALUE;
		for (TransitStopFacility stop : this.stops.values()) {
			double distance = walkBeelineFactor * CoordUtils.calcEuclideanDistance(coord, stop.getCoord());
			if (distance <= drtconfig.getMaxWalkDistance()) {
				if (distance<bestDist){
					bestDist = distance;
					bestStop = stop;
				}
				
			}

		}
		return bestStop;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return drtStageActivityType;
	}

	Coord getFacilityCoord(Facility<?> facility) {
		Coord coord = facility.getCoord();
		if (coord == null) {
			coord = network.getLinks().get(facility.getLinkId()).getCoord();
			if (coord == null)
				throw new RuntimeException("From facility has neither coordinates nor link Id. Should not happen.");
		}
		return coord;
	}


}
