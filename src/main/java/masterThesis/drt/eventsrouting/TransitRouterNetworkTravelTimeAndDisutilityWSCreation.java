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
import com.google.inject.name.Named;
import masterThesis.drt.eventsrouting.stopstoptime.StopStopTime;
import masterThesis.drt.eventsrouting.waitstoptime.WaitTime;
import masterThesis.drt.run.DrtConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * TravelTime and TravelDisutility calculator to be used with the transit network used for transit routing.
 * This version considers waiting time at stops, and takes travel time between stops from a {@link StopStopTime} object.
 *
 * @author sergioo
 */
public class TransitRouterNetworkTravelTimeAndDisutilityWSCreation extends TransitRouterNetworkTravelTimeAndDisutilityWS  {

    public TransitRouterNetworkTravelTimeAndDisutilityWSCreation(TransitRouterCommonDisutility delegate, DrtTransitRouterConfig config) {
        super(delegate, config);
    }

    @Override
    protected boolean isWaiting() {
        return false;
    }
}