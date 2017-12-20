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

package masterThesis.drt.passenger.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Request;

import java.util.Map;

/**
 * @author michalm
 */

public class DrtRequestRejectedEvent extends Event {

	public static final String EVENT_TYPE = "DrtRequest rejected";

	public static final String ATTRIBUTE_REQUEST = "request";

	private final Id<Request> requestId;

	private final Id<Person> personId;

	public DrtRequestRejectedEvent(double time, Id<Request> requestId, Id<Person> personId) {
		super(time);
		this.requestId = requestId;
		this.personId = personId;
	}

	@Override
	
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	/**
	 *  the ID of the initial request submitted
	 */
	public Id<Request> getRequestId() {
		return requestId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_REQUEST, requestId + "");
		return attr;
	}
}
