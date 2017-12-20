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
import masterThesis.drt.eventsrouting.stopstoptime.StopStopTime;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.*;
import org.matsim.vehicles.Vehicle;

/**
 * TravelTime and TravelDisutility calculator to be used with the transit network used for transit routing.
 * This version considers waiting time at stops, and takes travel time between stops from a {@link StopStopTime} object.
 *
 * @author sergioo
 */
public class TransitRouterNetworkTravelTimeAndDisutilityWS implements TransitTravelDisutility, TravelDisutility, TravelTime {

	private Link previousLink;
	private double previousTime;
	private double cachedLinkTime;
	private TransitRouterCommonDisutility delegate;
	private DrtTransitRouterConfig config;

	public TransitRouterNetworkTravelTimeAndDisutilityWS(TransitRouterCommonDisutility delegate,DrtTransitRouterConfig config){
		this.delegate = delegate;
		this.config = config;
	}
	@Override
	public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
		previousLink = link;
		previousTime = time;
		TransitRouterNetworkWW.TransitRouterNetworkLink wrapped = (TransitRouterNetworkWW.TransitRouterNetworkLink) link;
		if (!wrapped.fromNode.getId().toString().endsWith("_W"))
			//in line link
			cachedLinkTime = delegate.getTravelTime(wrapped.getId(), time);
		else
			//wait link
			cachedLinkTime = delegate.getWaitingTime(wrapped.getId(), time);
		return cachedLinkTime;
	}
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
		return this.getLinkTravelDisutility(link,time,person,vehicle);
	}
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		boolean cachedTravelDisutility = false;
		if(previousLink==link && previousTime==time)
			cachedTravelDisutility = true;
		TransitRouterNetworkWW.TransitRouterNetworkLink wrapped = (TransitRouterNetworkWW.TransitRouterNetworkLink) link;
		if (!wrapped.fromNode.getId().toString().endsWith("_W"))
			return -(cachedTravelDisutility?cachedLinkTime:delegate.getTravelTime(wrapped.getId(),time))*this.config.getMarginalUtilityOfTravelTimeDrt_utl_s()
					- link.getLength() * (this.config.getMarginalUtilityOfTravelDistanceDrt_utl_m());
		else
			// it's a wait link
			return isWaiting()?(-(cachedTravelDisutility?cachedLinkTime:delegate.getWaitingTime(wrapped.getId(),time))*this.config.getMarginalUtilityOfWaitingDrt_utl_s()):0;
	}

	protected boolean isWaiting() {
		return true;
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
	public double getWalkTravelTime(Person person, Coord coord, Coord toCoord) {
		double distance = CoordUtils.calcEuclideanDistance(coord, toCoord);
		double initialTime = distance / config.getBeelineWalkSpeed();
		return initialTime;
	}
}
