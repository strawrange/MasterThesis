/* *********************************************************************** *
 * project: org.matsim.*
 * TranitRouterVariableImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
import com.google.inject.Injector;
import com.google.inject.name.Named;
import masterThesis.drt.closerouting.DrtStageActivityType;
import masterThesis.drt.run.DrtConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.MultiNodeDijkstra;
import org.matsim.pt.router.MultiNodeDijkstra.InitialNode;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.omg.SendingContext.RunTime;

import java.util.*;

public class TransitRouterVariableImpl implements RoutingModule {

	private final TransitRouterNetworkWW transitNetwork;
	private TransitRouterConfig config;
	private TransitRouterNetworkTravelTimeAndDisutilityWS ttCalculator;

    private final MultiNodeDijkstra dijkstra;
    private final String mode;



	public TransitRouterVariableImpl(DrtTransitRouterConfig config, final TransitRouterNetworkWW routerNetwork, String mode,TransitRouterCommonDisutility delegate) {
		this.transitNetwork = routerNetwork;
		this.mode = mode;
		if(this.mode.equals(DrtConfigGroup.DRT_MODE)) {
			this.ttCalculator = new TransitRouterNetworkTravelTimeAndDisutilityWS(delegate,config);
		}
		else {
			this.ttCalculator = new TransitRouterNetworkTravelTimeAndDisutilityWSCreation(delegate,config);
		}
		this.dijkstra = new MultiNodeDijkstra(this.transitNetwork, this.ttCalculator, this.ttCalculator);
		this.config = config;
	}
	
	private Map<Node, InitialNode> locateWrappedNearestTransitNodes(Person person, Coord coord, double departureTime){
		Collection<TransitRouterNetworkWW.TransitRouterNetworkNode> nearestNodes = this.transitNetwork.getNearestNodes(coord, this.config.getSearchRadius());
		if (nearestNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkWW.TransitRouterNetworkNode nearestNode = this.transitNetwork.getNearestNode(coord);
			double distance = CoordUtils.calcEuclideanDistance(coord, nearestNode.stop.getCoord());
			nearestNodes = this.transitNetwork.getNearestNodes(coord, distance + this.config.getExtensionRadius());
		}
		Map<Node, InitialNode> wrappedNearestNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkWW.TransitRouterNetworkNode node : nearestNodes) {
			Coord toCoord = node.stop.getCoord();
			double initialTime = getWalkTime(person, coord, toCoord);
			double initialCost = getWalkDisutility(person, coord, toCoord);
			wrappedNearestNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}
		return wrappedNearestNodes;
	}
	
	private double getWalkTime(Person person, Coord coord, Coord toCoord) {
		return this.ttCalculator.getWalkTravelTime(person, coord, toCoord);
	}
	
	private double getWalkDisutility(Person person, Coord coord, Coord toCoord) {
		return this.ttCalculator.getWalkTravelDisutility(person, coord, toCoord);
	}
	@Override
	public List<? extends PlanElement> calcRoute(final Facility<?> fromFacility, final Facility<?> toFacility, final double departureTime, final Person person) {
		// find possible start stops
		Map<Node, InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNodes(person, fromFacility.getCoord(), departureTime);
		// find possible end stops
		Map<Node, InitialNode> wrappedToNodes  = this.locateWrappedNearestTransitNodes(person, toFacility.getCoord(), departureTime);

		// find routes between start and end stops
		Path p = this.dijkstra.calcLeastCostPath(wrappedFromNodes, wrappedToNodes, person);
		if (p == null) {
			return null;
		}

		double directWalkCost =  CoordUtils.calcEuclideanDistance(fromFacility.getCoord(), toFacility.getCoord()) < 800 ?
				-CoordUtils.calcEuclideanDistance(fromFacility.getCoord(), toFacility.getCoord()) / this.config.getBeelineWalkSpeed() *
                this.config.getMarginalUtilityOfTravelTimeWalk_utl_s(): Double.MAX_VALUE;
		double pathCost = Double.MAX_VALUE;
		if (p.travelTime != 0){
			pathCost = p.travelCost + wrappedFromNodes.get(p.nodes.get(0)).initialCost + wrappedToNodes.get(p.nodes.get(p.nodes.size() - 1)).initialCost;
		}
		if (directWalkCost < pathCost || p.travelTime == 0) {
			List<Leg> legs = new ArrayList<Leg>();
			Leg leg = PopulationUtils.createLeg(TransportMode.walk);
			double walkDistance = CoordUtils.calcEuclideanDistance(fromFacility.getCoord(), toFacility.getCoord());
			Route walkRoute = RouteUtils.createGenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
			walkRoute.setDistance(walkDistance);
			leg.setRoute(walkRoute);
			leg.setTravelTime(walkDistance/this.config.getBeelineWalkSpeed());
			legs.add(leg);
			return legs;
		}

		List<Leg> DrtLegs = convertPathToLegList( departureTime, p, fromFacility.getCoord(), toFacility.getCoord(), person, fromFacility.getLinkId(), toFacility.getLinkId()) ;

		return fillWithActivities(DrtLegs, fromFacility, toFacility, departureTime,person);
	}

    private List<PlanElement> fillWithActivities(
            final List<Leg> baseTrip,
            final Facility fromFacility,
            final Facility toFacility, double departureTime, Person person) {
        List<PlanElement> trip = new ArrayList<>();
        Coord nextCoord = null;
        boolean firstLeg = true;
        ExperimentalTransitRoute tRoute = null;
        for (Leg leg : baseTrip) {
            if (firstLeg){
                firstLeg = false;
            } else{
                if (leg.getRoute() instanceof ExperimentalTransitRoute) {
                    tRoute = (ExperimentalTransitRoute) leg.getRoute();
                    Activity act = PopulationUtils.createActivityFromLinkId(DrtStageActivityType.DRTSTAGEACTIVITY, tRoute.getStartLinkId());
                    act.setFacilityId(transitStopFacilityIdToActivity(tRoute.getAccessStopId()));
                    act.setMaximumDuration(0.0);
                    trip.add(act);
                } else {
                    Activity act = PopulationUtils.createActivityFromLinkId(DrtStageActivityType.DRTSTAGEACTIVITY, leg.getRoute().getStartLinkId());
                    act.setMaximumDuration(0.0);
                    if (tRoute ==null){
                        throw new RuntimeException("tRoute should be assigned first, order is wrong!");
                    }
                    act.setFacilityId(transitStopFacilityIdToActivity(tRoute.getEgressStopId()));
                    trip.add(act);
                }
            }
            trip.add(leg);
        }
        return trip;
    }

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return new StageActivityTypesImpl(DrtStageActivityType.DRTSTAGEACTIVITY);
	}
	
	protected List<Leg> convertPathToLegList( double departureTime, Path p, Coord fromCoord, Coord toCoord, Person person, Id<Link> startLinkId, Id<Link> endLinkId) {
		List<Leg> legs = new ArrayList<Leg>();
		Leg leg;
		double walkDistance, walkWaitTime, travelTime = 0;
		Route walkRoute;
		Coord coord = fromCoord;
		double time = departureTime;
		Id<Link> linkId = startLinkId;
		for (Link link : p.links) {
			TransitRouterNetworkWW.TransitRouterNetworkLink l = (TransitRouterNetworkWW.TransitRouterNetworkLink) link;
			if(!l.getFromNode().getId().toString().endsWith("_W")) {
				//travel link
				double ttime = ttCalculator.getLinkTravelTime(l, time, person, null);
				leg = PopulationUtils.createLeg(mode);
				Route ptRoute = new ExperimentalTransitRoute(l.fromNode.stop, l.toNode.stop,Id.create(0,TransitLine.class),Id.create(0, TransitRoute.class));
				leg.setRoute(ptRoute);
				leg.setTravelTime(ttime);
				legs.add(leg);
				time += ttime;
				coord = l.toNode.stop.getCoord();
				linkId = l.toNode.stop.getLinkId();
			}
			else {
				//wait link
				leg = PopulationUtils.createLeg(TransportMode.access_walk);
				walkDistance = CoordUtils.calcEuclideanDistance(coord, l.toNode.stop.getCoord());
				walkWaitTime = walkDistance/this.config.getBeelineWalkSpeed()/*+ttCalculator.getLinkTravelTime(l, time+walkDistance/this.config.getBeelineWalkSpeed(), person, null)*/;
				walkRoute = RouteUtils.createGenericRouteImpl(l.fromNode.stop.getLinkId(), l.toNode.stop.getLinkId());
				walkRoute.setDistance(walkDistance);
				leg.setRoute(walkRoute);
				leg.setTravelTime(walkWaitTime);
				legs.add(leg);
				time += walkWaitTime;
				coord = l.toNode.stop.getCoord();
				linkId = l.toNode.stop.getLinkId();
			}
			
		}
		leg = PopulationUtils.createLeg(TransportMode.access_walk);
		walkDistance = CoordUtils.calcEuclideanDistance(coord, toCoord); 
		walkWaitTime = walkDistance/this.config.getBeelineWalkSpeed();
		walkRoute = RouteUtils.createGenericRouteImpl(linkId, endLinkId);
		walkRoute.setDistance(walkDistance);
		leg.setRoute(walkRoute);
		leg.setTravelTime(walkWaitTime);
		legs.add(leg);
		return legs;
	}

	public TransitRouterNetworkWW getTransitRouterNetwork() {
		return this.transitNetwork;
	}

	protected TransitRouterNetworkWW getTransitNetwork() {
		return transitNetwork;
	}

	protected MultiNodeDijkstra getDijkstra() {
		return dijkstra;
	}

	protected TransitRouterConfig getConfig() {
		return config;
	}

    private Id<ActivityFacility> transitStopFacilityIdToActivity(Id<TransitStopFacility> transitStopFacilityId){
        return Id.create(transitStopFacilityId.toString(),ActivityFacility.class);
    }

}
