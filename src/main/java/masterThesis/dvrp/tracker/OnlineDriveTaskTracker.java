/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package masterThesis.dvrp.tracker;

import masterThesis.dvrp.path.VrpPathWithTravelData;
import masterThesis.dvrp.util.LinkTimePair;
import org.matsim.api.core.v01.network.Link;

/**
 * @author michalm
 */
public interface OnlineDriveTaskTracker extends TaskTracker {
	LinkTimePair getDiversionPoint();

	void divertPath(VrpPathWithTravelData newSubPath);

	void movedOverNode(Link nextLink);
}
