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

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.facilities.Facility;

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
