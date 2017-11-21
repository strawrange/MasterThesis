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

package masterThesis.dvrp.optimizer;

import masterThesis.dvrp.data.Vehicle;
import org.matsim.api.core.v01.network.Link;

/**
 * @author michalm
 * @author (of documentation) nagel
 */
public interface VrpOptimizerWithOnlineTracking extends VrpOptimizer {
	/**
	 * Notifies the optimizer that the next link was entered.
	 */
	void vehicleEnteredNextLink(Vehicle vehicle, Link nextLink);
}
