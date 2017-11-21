/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.example;

import static org.junit.Assert.*;

import masterThesis.drt.run.DrtConfigGroup;
import masterThesis.drt.scoring.AVScoringFunctionFactory;
import org.apache.commons.lang.mutable.MutableDouble;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import masterThesis.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.util.Collection;
import java.util.Map;

/**
 * @author nagel
 *
 */
public class HelloWorldTest {

	/**
	 * Test method for
	 */
	@Test
    public static void main(String[] args) {
		EventsManagerImpl eventsManager = new EventsManagerImpl();
		new MatsimEventsReader(eventsManager).readFile("/home/biyu/IdeaProjects/MasterThesis/output/A20_B20_C_D_poolScore/drt_5_10prct/output_events.xml.gz");


	}

	public static Coord getFacilityCoord(Facility<?> facility, Network network) {
		Coord coord = facility.getCoord();
		if (coord == null) {
			coord = network.getLinks().get(facility.getLinkId()).getCoord();
			if (coord == null)
				throw new RuntimeException("From facility has neither coordinates nor link Id. Should not happen.");
		}
		System.out.println(facility.getLinkId() + "  " + facility.getCoord());
		return coord;
	}


	public static Network createNetwork(Scenario scenario){
		Network network = scenario.getNetwork();
		return network;
	}



}
