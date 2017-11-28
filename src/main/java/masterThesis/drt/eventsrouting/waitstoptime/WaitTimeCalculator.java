/* *********************************************************************** *
 * project: org.matsim.*
 * WaitTimeCalculator.java
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

package masterThesis.drt.eventsrouting.waitstoptime;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import masterThesis.drt.run.DrtConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeData;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeDataArray;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * Save waiting times of agents while mobsim is running
 * 
 * @author sergioo
 */

@Singleton
public class WaitTimeCalculator implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler,VehicleArrivesAtFacilityEventHandler,  PersonStuckEventHandler, Provider<WaitTime> {

	//Attributes
	private final double timeSlot;
	private final Map<Id<TransitStopFacility>, WaitTimeData> waitTimes = new HashMap<>(1000);
	private final Map<Id<TransitStopFacility>, double[]> scheduledWaitTimes = new HashMap<>(1000);
	private final Map<Id<Person>, Double> agentsWaitingData = new HashMap<Id<Person>, Double>();
	private final Map<Id<Link>, Id<TransitStopFacility>> stopOfLink = new HashMap<>(1000);
	private final Map<Id<Vehicle>,Id<TransitStopFacility>> stopOfVehicle = new HashMap<>(1000);

	//Constructors
	@Inject
	public WaitTimeCalculator(@Named(DrtConfigGroup.DRT_MODE) final TransitSchedule transitSchedule, final Config config, EventsManager eventsManager) {
		this(transitSchedule, config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim().getEndTime()-config.qsim().getStartTime()));
		for (TransitStopFacility stop:transitSchedule.getFacilities().values()){
			stopOfLink.put(stop.getLinkId(),stop.getId());
		}
		eventsManager.addHandler(this);
	}
	public WaitTimeCalculator(final TransitSchedule transitSchedule, final int timeSlot, final int totalTime) {
		this.timeSlot = timeSlot;
		for(TransitStopFacility stop:transitSchedule.getFacilities().values()) {
			waitTimes.put(stop.getId(), new WaitTimeDataArray((int) (totalTime/timeSlot)+1));
			double[] cacheWaitTimes = new double[(int) (totalTime/timeSlot)+1];
			for(int i=0; i<cacheWaitTimes.length; i++)
				cacheWaitTimes[i] = 0;
			scheduledWaitTimes.put(stop.getId(), cacheWaitTimes);
		}
	}

	//Methods
	@Override
	public WaitTime get() {
		return new WaitTime() {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public double getRouteStopWaitTime(Id<TransitStopFacility> stopId, double time) {
				return WaitTimeCalculator.this.getRouteStopWaitTime(stopId, time);
			}
		};
	}
	private double getRouteStopWaitTime(Id<TransitStopFacility> stopId, double time) {
		WaitTimeData waitTimeData = waitTimes.get(stopId);
		if(waitTimeData.getNumData((int) (time/timeSlot))==0) {
			double[] waitTimes = scheduledWaitTimes.get(stopId);
			return waitTimes[(int) (time/timeSlot)<waitTimes.length?(int) (time/timeSlot):(waitTimes.length-1)];
		}
		else
			return waitTimeData.getWaitTime((int) (time/timeSlot));
	}
	@Override
	public void reset(int iteration) {
		for(WaitTimeData waitTimeData:waitTimes.values())
			waitTimeData.resetWaitTimes();
		agentsWaitingData.clear();
	}
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(event.getLegMode().startsWith(DrtConfigGroup.DRT_MODE) && agentsWaitingData.get(event.getPersonId())==null)
			agentsWaitingData.put(event.getPersonId(), event.getTime());
		else if(agentsWaitingData.get(event.getPersonId())!=null)
			new RuntimeException("Departing with old data");
	}
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Double startWaitingTime = agentsWaitingData.get(event.getPersonId());
		if(startWaitingTime!=null) {
			WaitTimeData data = waitTimes.get(stopOfVehicle.get(event.getVehicleId()));
			data.addWaitTime((int) (startWaitingTime/timeSlot), event.getTime()-startWaitingTime);
			agentsWaitingData.remove(event.getPersonId());
		}
	}



	@Override
	public void handleEvent(PersonStuckEvent event) {
		Double startWaitingTime = agentsWaitingData.get(event.getPersonId());
		if(startWaitingTime!=null) {
			WaitTimeData data = waitTimes.get(stopOfLink.get(event.getLinkId()));
			data.addWaitTime((int) (startWaitingTime/timeSlot), 2*(event.getTime()-startWaitingTime));
			agentsWaitingData.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		stopOfVehicle.put(event.getVehicleId(),event.getFacilityId());
	}
}
