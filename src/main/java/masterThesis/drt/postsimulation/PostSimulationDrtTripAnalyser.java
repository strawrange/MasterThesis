package masterThesis.drt.postsimulation;

import masterThesis.drt.run.DrtConfigGroup;
import masterThesis.drt.scoring.AVRecord;
import masterThesis.drt.scoring.EventLists;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.eventsBasedPTRouter.vehicleOccupancy.VehicleOccupancy;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV2;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static masterThesis.drt.postsimulation.EventHandlerForDrtTrips.drtTrips;


public class PostSimulationDrtTripAnalyser {
    private static int ITER = 10;
    private static String FOLDER = "/home/biyu/IdeaProjects/MasterThesis/output/drt_10_10prct_two_modes_creation_penalty30_changeSingleTripMode_alwaysCreate_0.1modeChange_vehicleKill_abort_abortTime_transitRouter_waitTimeCalculator_expWalkScore_100_DisutilityCreation_WaitTimeDebug_update30_noExpRideShareBonus/ITERS/it." + ITER + "/";
    private static String EVENTSFILE =  FOLDER + ITER + ".events.xml.gz";
    private static String NETWORKSFILE = "/home/biyu/IdeaProjects/MasterThesis/scenarios/siouxFalls/network.xml";

    public static void main(String[] args) throws IOException {
        EventsManager manager = EventsUtils.createEventsManager();
        Network network = NetworkUtils.createNetwork();
        new NetworkReaderMatsimV2(network).readFile(NETWORKSFILE);
        EventHandlerForDrtTrips eventHandlerForDrtTrips = new EventHandlerForDrtTrips();
        manager.addHandler(eventHandlerForDrtTrips);
        new MatsimEventsReader(manager).readFile(EVENTSFILE);
        writeDrtTripFile(network);
    }

    public static void writeDrtTripFile(Network network) throws IOException {
        BufferedWriter bw = IOUtils.getBufferedWriter(FOLDER + "drtTripsAnalysis.csv");
        bw.write("ID;personId;vehicleId;fromLinkId;toLinkId;fromX;fromY;toX;toY;fromAng;toAng;departureTime;arrivalTime;waitTime;travelTime;maxPax");
        int i = 0;
        for (DrtTrip drtTrip: drtTrips.values()){
            bw.newLine();
            Link fromLink = network.getLinks().get(drtTrip.fromLinkId);
            Coord fromCoord = fromLink.getCoord();
            Coord fromfromCoord = fromLink.getFromNode().getCoord();
            Coord fromtoCoord = fromLink.getToNode().getCoord();
            double fromAng = Math.atan2(fromtoCoord.getY()-fromfromCoord.getY(),fromtoCoord.getX()-fromfromCoord.getX());
            Link toLink = network.getLinks().get(drtTrip.toLinkId);
            Coord toCoord = toLink.getCoord();
            Coord tofromCoord = toLink.getFromNode().getCoord();
            Coord totoCoord = toLink.getToNode().getCoord();
            double toAng = Math.atan2(totoCoord.getY()-tofromCoord.getY(),totoCoord.getX()-tofromCoord.getX());
            bw.write(i++ + ";" + drtTrip.personId.toString() + ";" +drtTrip.vehicleId.toString() + ";" +drtTrip.fromLinkId.toString() + ";" +
                    drtTrip.toLinkId.toString() + ";" + fromCoord.getX() + ";" +fromCoord.getY()  + ";" + fromAng + ";" + toAng + ";" + toCoord.getX() + ";" + toCoord.getY()  + ";" +
            drtTrip.departureTime + ";" + drtTrip.arrivalTime + ";" + drtTrip.waitTime + ";" + drtTrip.travelTime + ";" + drtTrip.maxPax);
        }
        bw.close();
    }
}

class EventHandlerForDrtTrips implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,  PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler{
    static public Map<Id<DrtTrip>,DrtTrip> drtTrips = new HashMap<>();
    static public Map<Id<Vehicle>, EventLists> vehicles = new HashMap<>();

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (event.getPersonId().toString().startsWith("AV")){
            return;
        }
        DrtTrip drtTrip;
        if (drtTrips.containsKey(Id.create(event.getPersonId().toString() + "_" + 1, DrtTrip.class))) {
            drtTrip = drtTrips.get(Id.create(event.getPersonId().toString() + "_" + 1, DrtTrip.class));
        }else{
            drtTrip = drtTrips.get(Id.create(event.getPersonId().toString() + "_" + 0, DrtTrip.class));
        }
        drtTrip.waitTime = event.getTime() - drtTrip.departureTime;
        drtTrip.vehicleId = event.getVehicleId();
        EventLists eventLists = new EventLists();
        if (this.vehicles.containsKey(event.getVehicleId())){
            eventLists = vehicles.get(event.getVehicleId());
        }
        eventLists.add(event);
        this.vehicles.put(event.getVehicleId(),eventLists);
    }


    @Override
    public void reset(int iteration) {

    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (event.getLegMode().startsWith(DrtConfigGroup.DRT_MODE)) {
            DrtTrip drtTrip;
            if (drtTrips.containsKey(Id.create(event.getPersonId().toString() + "_" + 1, DrtTrip.class))) {
                drtTrip = drtTrips.get(Id.create(event.getPersonId().toString() + "_" + 1, DrtTrip.class));
            } else {
                drtTrip = drtTrips.get(Id.create(event.getPersonId().toString() + "_" + 0, DrtTrip.class));
            }
            drtTrip.toLinkId = event.getLinkId();
            drtTrip.arrivalTime = event.getTime();
            drtTrip.travelTime = drtTrip.arrivalTime - drtTrip.departureTime;
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().startsWith(DrtConfigGroup.DRT_MODE)){
            DrtTrip drtTrip = new DrtTrip(event.getPersonId(), event.getLinkId(),event.getTime());
            if (drtTrips.containsKey(Id.create(event.getPersonId().toString() + "_" + 0, DrtTrip.class))){
                drtTrips.put(Id.create(event.getPersonId().toString() + "_" + 1, DrtTrip.class),drtTrip);
            }else{
                drtTrips.put(Id.create(event.getPersonId().toString() + "_" + 0, DrtTrip.class),drtTrip);
            }

        }
    }

    @Override
    public void handleEvent(PersonStuckEvent event) {
        if (event.getLegMode().startsWith(DrtConfigGroup.DRT_MODE)){
            if (drtTrips.containsKey(Id.create(event.getPersonId().toString() + "_" + 1, DrtTrip.class))) {
                drtTrips.remove(Id.create(event.getPersonId().toString() + "_" + 1, DrtTrip.class));
            } else {
                drtTrips.remove(Id.create(event.getPersonId().toString() + "_" + 0, DrtTrip.class));
            }
        }
    }


    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (event.getPersonId().toString().startsWith("AV")){
            return;
        }
        DrtTrip drtTrip;
        if (drtTrips.containsKey(Id.create(event.getPersonId().toString() + "_" + 1, DrtTrip.class))) {
            drtTrip = drtTrips.get(Id.create(event.getPersonId().toString() + "_" + 1, DrtTrip.class));
        }else{
            drtTrip = drtTrips.get(Id.create(event.getPersonId().toString() + "_" + 0, DrtTrip.class));
        }
        if (!this.vehicles.containsKey( event.getVehicleId())){
            throw new RuntimeException("vehicles do not register in the list!");
        }
        EventLists eventLists = this.vehicles.get( event.getVehicleId());
        eventLists.add(event);
        int seats = 0;
        double maxSeats = 0;
        boolean onBoard = false;
        for(int i = 0; i < eventLists.size(); i++){
            Event e = eventLists.getEvents().get(i);
            if (onBoard){
                maxSeats=Double.max(seats,maxSeats);
            }
            if (e instanceof PersonEntersVehicleEvent){
                seats++;
                if (((PersonEntersVehicleEvent) e).getPersonId().equals( event.getPersonId())){
                    onBoard = true;
                }
            }
            if (e instanceof PersonLeavesVehicleEvent){
                seats--;
                if (seats == 0){
                    eventLists.clear();
                }
            }
        }
        drtTrip.maxPax = maxSeats;
    }
}

class DrtTrip{
    Id<Person> personId;
    Id<Vehicle> vehicleId;
    Id<Link> fromLinkId;
    Id<Link> toLinkId;

    double departureTime;
    double arrivalTime;
    double waitTime;
    double travelTime;
    double maxPax = 0;

    public DrtTrip(Id<Person> personId, Id<Link> fromLinkId, double departureTime){
        this.personId = personId;
        this.fromLinkId = fromLinkId;
        this.departureTime = departureTime;
    }
}

