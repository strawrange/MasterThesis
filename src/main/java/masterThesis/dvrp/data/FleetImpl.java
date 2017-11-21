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

package masterThesis.dvrp.data;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author michalm
 */
public class FleetImpl implements Fleet {
	private final Map<Id<Vehicle>, Vehicle> vehicles  = new LinkedHashMap<>();
	private final double serviceEndTime;

    public FleetImpl(double serviceEndTime) {
        this.serviceEndTime = serviceEndTime;
    }
    @Override
	public Map<Id<Vehicle>, ? extends Vehicle> getVehicles() {
		return Collections.unmodifiableMap(vehicles);
	}

	public Map<Id<Vehicle>, Vehicle> getModifiableVehicles(){
        return vehicles;
    }

	public void addVehicle(Vehicle vehicle) {
		vehicles.put(vehicle.getId(), vehicle);
	}

	public void resetSchedules() {
		for (Vehicle v : vehicles.values()) {
			v.resetSchedule();
		}
	}

	public double getServiceEndTime(){
        return this.serviceEndTime;
    }

    @Override
    public void initialize() {
        vehicles.clear();
    }

}
