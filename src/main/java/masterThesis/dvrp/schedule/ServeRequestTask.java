/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package masterThesis.dvrp.schedule;

import masterThesis.dvrp.data.Request;
import org.matsim.api.core.v01.network.Link;

public class ServeRequestTask extends StayTaskImpl {
	private final Request request;

	public ServeRequestTask(double beginTime, double endTime, Link link, Request request) {
		super(beginTime, endTime, link);
		this.request = request;
	}

	public Request getRequest() {
		return request;
	}
}
