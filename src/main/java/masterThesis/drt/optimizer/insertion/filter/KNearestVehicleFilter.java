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

/**
 * 
 */
package masterThesis.drt.optimizer.insertion.filter;

import masterThesis.drt.data.DrtRequest;
import masterThesis.drt.optimizer.VehicleData;
import org.matsim.api.core.v01.network.Link;
import masterThesis.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.util.PartialSort;
import org.matsim.contrib.util.distance.DistanceUtils;

import java.util.List;

/**
 * @author  jbischoff
 *
 */

/**
 *	filters out the k nearest vehicles to a request.
 */
public class KNearestVehicleFilter implements DrtVehicleFilter {

	
	final int k;
	
	
	public KNearestVehicleFilter(int k) {
		this.k = k;
	}


	/* (non-Javadoc)
	 * @see org.matsim.contrib.drt.optimizer.insertion.filter.DrtVehicleFilter#applyFilter(org.matsim.contrib.drt.data.DrtRequest, org.matsim.contrib.drt.optimizer.VehicleData)
	 */
	@Override
	public List<VehicleData.Entry> applyFilter(DrtRequest drtRequest, VehicleData vData) {
		Link toLink = drtRequest.getFromLink();
        PartialSort<Entry> nearestVehicleSort = new PartialSort<Entry>(k);

        for (Entry veh : vData.getEntries()) {
            double squaredDistance = DistanceUtils.calculateSquaredDistance(veh.start.link, toLink);
            nearestVehicleSort.add(veh, squaredDistance);
        }

        return nearestVehicleSort.retriveKSmallestElements();
		
	}

}
