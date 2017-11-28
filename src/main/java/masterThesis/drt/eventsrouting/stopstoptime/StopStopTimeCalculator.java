package masterThesis.drt.eventsrouting.stopstoptime;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import masterThesis.drt.run.DrtConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeData;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeDataArray;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Singleton
public class StopStopTimeCalculator implements VehicleArrivesAtFacilityEventHandler, PersonLeavesVehicleEventHandler, Provider<StopStopTime> {

	private final Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, StopStopTimeData>> stopStopTimes = new HashMap<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, StopStopTimeData>>(5000);
	private final Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, Double>> scheduledStopStopTimes = new HashMap<>(5000);
	private final Map<Id<Vehicle>, Tuple<Id<TransitStopFacility>, Double>> inTransitVehicles = new HashMap<>(1000);
	private final Set<Id<Vehicle>> vehicleIds = new HashSet<>();
	private double timeSlot;
	private boolean useVehicleIds = false;

	//Constructors
	@Inject
	public StopStopTimeCalculator(@Named(DrtConfigGroup.DRT_MODE) final TransitSchedule transitSchedule, final Config config, EventsManager eventsManager) {
		this(transitSchedule, config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim().getEndTime()-config.qsim().getStartTime()));
		eventsManager.addHandler(this);
	}
	public StopStopTimeCalculator(final TransitSchedule transitSchedule, final int timeSlot, final int totalTime) {
		this.timeSlot = timeSlot;
		Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, Integer>> numObservations = new HashMap<>();
		for(TransitStopFacility stopA:transitSchedule.getFacilities().values()) {
			Map<Id<TransitStopFacility>, StopStopTimeData> map = new HashMap<>(2);
			stopStopTimes.put(stopA.getId(), map);
			for (TransitStopFacility stopB : transitSchedule.getFacilities().values())
				if (stopA != stopB) {
					map.put(stopB.getId(), new StopStopTimeDataArray((int) (totalTime / timeSlot) + 1));
					Map<Id<TransitStopFacility>, Double> map2 = scheduledStopStopTimes.get(stopA.getId());
					Double stopStopTime;
					if (map2 == null) {
						map2 = new HashMap<>(2);
						scheduledStopStopTimes.put(stopA.getId(), map2);
						stopStopTime = 0.0;
					} else {
						stopStopTime = map2.get(stopB.getId());
						if (stopStopTime == null)
							stopStopTime = 0.0;
					}
					map2.put(stopB.getId(), stopStopTime + CoordUtils.calcEuclideanDistance(stopA.getCoord(), stopB.getCoord()) / (17.0 / 3.6));
				}
		}
	}

	//Methods
	private double getStopStopTime(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
		StopStopTimeData stopStopTimeData = stopStopTimes.get(stopOId).get(stopDId);
		if(stopStopTimeData.getNumData((int) (time/timeSlot))==0)
			return scheduledStopStopTimes.get(stopOId).get(stopDId);
		else
			return stopStopTimeData.getStopStopTime((int) (time/timeSlot));
	}
	private double getStopStopTimeVariance(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
		StopStopTimeData stopStopTimeData = stopStopTimes.get(stopOId).get(stopDId);
		if(stopStopTimeData.getNumData((int) (time/timeSlot))==0)
			return 0;
		else
			return stopStopTimeData.getStopStopTimeVariance((int) (time/timeSlot));
	}
	@Override
	public void reset(int iteration) {
		for(Map<Id<TransitStopFacility>, StopStopTimeData> map:stopStopTimes.values())
			for(StopStopTimeData stopStopTimeData:map.values())
				stopStopTimeData.resetStopStopTimes();
		inTransitVehicles.clear();
	}
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if(!useVehicleIds || vehicleIds.contains(event.getVehicleId())) {
			Tuple<Id<TransitStopFacility>, Double> route = inTransitVehicles.remove(event.getVehicleId());
			if(route!=null)
				stopStopTimes.get(route.getFirst()).get(event.getFacilityId()).addStopStopTime((int) (route.getSecond()/timeSlot), event.getTime()-route.getSecond());
			inTransitVehicles.put(event.getVehicleId(), new Tuple<Id<TransitStopFacility>, Double>(event.getFacilityId(), event.getTime()));
		}
	}
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if((!useVehicleIds || vehicleIds.contains(event.getVehicleId())) && event.getPersonId().toString().startsWith("AV") && event.getPersonId().toString().contains(event.getVehicleId().toString()))
			inTransitVehicles.remove(event.getVehicleId());
	}

	public void setUseVehicleIds(boolean useVehicleIds) {
		this.useVehicleIds = useVehicleIds;
	}

	@Override
	public StopStopTime get() {
		return new StopStopTime() {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;
			@Override
			public double getStopStopTime(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
				return StopStopTimeCalculator.this.getStopStopTime(stopOId, stopDId, time);
			}
			@Override
			public double getStopStopTimeVariance(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
				return StopStopTimeCalculator.this.getStopStopTimeVariance(stopOId, stopDId, time);
			}
		};
	}
}
