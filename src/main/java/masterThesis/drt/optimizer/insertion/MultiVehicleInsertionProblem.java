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

package masterThesis.drt.optimizer.insertion;

import masterThesis.drt.data.DrtRequest;
import masterThesis.drt.optimizer.VehicleData;
import masterThesis.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import masterThesis.dvrp.data.Vehicle;

/**
 * @author michalm
 */
public class MultiVehicleInsertionProblem {
	private final SingleVehicleInsertionProblem insertionProblem;

	public MultiVehicleInsertionProblem(SingleVehicleInsertionProblem insertionProblem) {
		this.insertionProblem = insertionProblem;
	}

	public BestInsertion findBestInsertion(DrtRequest drtRequest, VehicleData vData) {
		return findBestInsertion(drtRequest, vData.getEntries());
	}

	public BestInsertion singleVehicleInsertion(DrtRequest drtRequest, VehicleData veh) {
		return insertionProblem.findBestInsertion(drtRequest,veh.getEntry(0));
	}

	// TODO run Dijkstra once for all vehicles instead of running it separately for each one
	public BestInsertion findBestInsertion(DrtRequest drtRequest, Iterable<VehicleData.Entry> vEntries) {
		double minCost = Double.MAX_VALUE;
		BestInsertion fleetBestInsertion = null;
		for (VehicleData.Entry vEntry : vEntries) {
			BestInsertion bestInsertion = insertionProblem.findBestInsertion(drtRequest, vEntry);
			if (bestInsertion.cost < minCost) {
				fleetBestInsertion = bestInsertion;
				minCost = bestInsertion.cost;
			}
		}
		return fleetBestInsertion;
	}
}
