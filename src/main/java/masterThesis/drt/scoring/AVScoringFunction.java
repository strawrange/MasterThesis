package masterThesis.drt.scoring;

import masterThesis.drt.run.DrtConfigGroup;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class AVScoringFunction implements  SumScoringFunction.ArbitraryEventScoring, org.matsim.core.scoring.SumScoringFunction.LegScoring {
    protected double score;
    public static final String FILE_NAME = "poolingStats";

    /** The parameters used for scoring */
    protected final ScoringParameters params;
    protected Network network;
    private boolean nextEnterPtIsFirstOfTrip = true;
    private boolean nextStartPtLegIsFirstOfTrip = true;
    private boolean currentLegIsDrtLeg = false;
    private boolean currentLegIsPtLeg = false;
    private double lastActivityEndTime = Time.UNDEFINED_TIME ;
    private static Map<Id<Vehicle>, EventLists> boardingTime = new HashMap<>();
    private Id<Person> personId;
    private ArrayList<AVRecord> table = new ArrayList<>();
    private int trips = 0;
    private static int ccc=0 ;
    private static BufferedWriter bw = null;
    private static int iteration = 0;


    public AVScoringFunction(final ScoringParameters params, Network network) {
        this.params = params;
        this.network = network;
        this.currentLegIsDrtLeg = false;
        if (bw == null){
            initializeBW();
        }
    }

    private void initializeBW() {
        bw = IOUtils.getBufferedWriter(FILE_NAME  + ".txt");
        try {
            bw.write("Iteration;LegID;PersonID;VehicleID;LegStartTime;LegEndTime;AvgOccupancy;MaxOccupancy;WaitScore;TravelScore;Bonus1;Bonus2;FinalScore1;FinalScore2");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleEvent(Event event) {
        if (event instanceof ActivityEndEvent) {

            if (!PtConstants.TRANSIT_ACTIVITY_TYPE.equals(((ActivityEndEvent) event).getActType())) {
                this.nextEnterPtIsFirstOfTrip = true;
                this.nextStartPtLegIsFirstOfTrip = true;
            }
            this.lastActivityEndTime = event.getTime();
        }

        if (event instanceof PersonEntersVehicleEvent && currentLegIsDrtLeg) {

            EventLists eventLists = new EventLists();
            if (this.boardingTime.containsKey(((PersonEntersVehicleEvent) event).getVehicleId())){
                eventLists = boardingTime.get(((PersonEntersVehicleEvent) event).getVehicleId());
            }
            eventLists.add(event);
            this.boardingTime.put(((PersonEntersVehicleEvent) event).getVehicleId(),eventLists);

            // add score of waiting, _minus_ score of travelling (since it is added in the legscoring above):
            this.score += (event.getTime() - this.lastActivityEndTime) * (this.params.marginalUtilityOfWaitingPt_s- this.params.modeParams.get(DrtConfigGroup.DRT_MODE).marginalUtilityOfTraveling_s);
            this.personId = ((PersonEntersVehicleEvent) event).getPersonId();
            AVRecord record = new AVRecord(Id.create(trips,Leg.class), (PersonEntersVehicleEvent) event,this.lastActivityEndTime, this.params.marginalUtilityOfWaitingPt_s);

            table.add(record);
        }

        if ( event instanceof PersonEntersVehicleEvent && currentLegIsPtLeg ) {
            if ( !this.nextEnterPtIsFirstOfTrip ) {
                // all vehicle entering after the first triggers the disutility of line switch:
                this.score  += params.utilityOfLineSwitch ;
            }
            this.nextEnterPtIsFirstOfTrip = false ;
            // add score of waiting, _minus_ score of travelling (since it is added in the legscoring above):
            this.score += (event.getTime() - this.lastActivityEndTime) * (this.params.marginalUtilityOfWaitingPt_s- this.params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s);
        }

        if (event instanceof PersonLeavesVehicleEvent && currentLegIsDrtLeg){
            if (!this.boardingTime.containsKey(((PersonLeavesVehicleEvent) event).getVehicleId())){
                throw new RuntimeException("vehicles do not register in the list!");
            }
            EventLists eventLists = this.boardingTime.get(((PersonLeavesVehicleEvent) event).getVehicleId());
            eventLists.add(event);
            int seats = 0;
            double sumSeats = 0;
            double sumTime = 0;
            double maxSeats = 0;
            double bonus = 0;
            boolean onBoard = false;
            for(int i = 0; i < eventLists.size(); i++){
                Event e = eventLists.getEvents().get(i);
                if (onBoard){
                    Event lastEvent = eventLists.getEvents().get(i-1);
                    double duration = e.getTime() - lastEvent.getTime();
                    maxSeats=Double.max(seats,maxSeats);
                    sumSeats += seats * duration;
                    sumTime += duration;
                }
                if (e instanceof PersonEntersVehicleEvent){
                    seats++;
                    if (((PersonEntersVehicleEvent) e).getPersonId().equals(((PersonLeavesVehicleEvent) event).getPersonId())){
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
            this.score -= sumTime * bonusCalculation(maxSeats);

            bonus = sumTime * bonusCalculation(maxSeats);
            if (!personId.equals(((PersonLeavesVehicleEvent) event).getPersonId())){
                throw new RuntimeException("Agent does not enter the vehicle!");
            }
            AVRecord record = table.get(trips);
            record.calculatePersonLeavesVehicleEvent(event.getTime(),sumSeats/sumTime,maxSeats,bonus,
                    this.params.modeParams.get(DrtConfigGroup.DRT_MODE).marginalUtilityOfTraveling_s,  this.score);
            trips++;
        }

        if ( event instanceof PersonDepartureEvent ) {
            this.currentLegIsPtLeg = TransportMode.pt.equals( ((PersonDepartureEvent)event).getLegMode() );
            if ( currentLegIsPtLeg ) {
                if ( !this.nextStartPtLegIsFirstOfTrip ) {
                    this.score -= params.modeParams.get(TransportMode.pt).constant ;
                    // (yyyy deducting this again, since is it wrongly added above.  should be consolidated; this is so the code
                    // modification is minimally invasive.  kai, dec'12)
                }
                this.nextStartPtLegIsFirstOfTrip = false ;
            }
        }


    }

    private double bonusCalculation(double maxSeats){
        double y = (- Math.exp(maxSeats - 5.36) + 16.013) / 3600;
        double bonus = - this.params.modeParams.get(DrtConfigGroup.DRT_MODE).marginalUtilityOfTraveling_s - y ;
        return - bonus;
    }

    @Override
    public void finish() {
        int i = 0;
        Id<Vehicle> id = Id.createVehicleId("AV" + i + "__" + this.personId);
        while (boardingTime.containsKey(id)) {
            this.score += boardingTime.get(id).getCount();
            i++;
            id = Id.createVehicleId("AV" + i + "__" + this.personId);
        }
        try {
            for (AVRecord record: table){
                bw.newLine();
                bw.write(iteration + ";" + record.getLegId().toString() + ";" + this.personId.toString() + ";" + record.getVehicleId().toString() + ";" +
                        record.getLegStartTime() + ";" + record.getLegEndTime() + ";" + record.getAvgOccupancy() + ";" + record.getMaxOccupancy() + ";" +
                                record.getWaitingScore() + ";" + record.getTravellingScore() + ";" + record.getBonus1() + ";" + record.getBonus2() + ";" +
                        record.getFinalScore1() + ";" + record.getFinalScore2()
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public double getScore() {
        return this.score;
    }

    @Override
    public void handleLeg(Leg leg) {
        double legScore = calcLegScore(leg.getDepartureTime(), leg.getDepartureTime() + leg.getTravelTime(), leg);
        this.score += legScore;
    }

    protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
        double tmpScore = 0.0;
        double travelTime = arrivalTime - departureTime; // travel time in seconds
        ModeUtilityParameters modeParams = this.params.modeParams.get(leg.getMode());
        if (modeParams == null) {
            if (leg.getMode().equals(TransportMode.transit_walk) || leg.getMode().equals(TransportMode.access_walk)
                    || leg.getMode().equals(TransportMode.egress_walk) ) {
                modeParams = this.params.modeParams.get(TransportMode.walk);
            } else {
//				modeParams = this.params.modeParams.get(TransportMode.other);
                throw new RuntimeException("just encountered mode for which no scoring parameters are defined: " + leg.getMode().toString() ) ;
            }
        }
        if (leg.getMode().equals(TransportMode.transit_walk) || leg.getMode().equals(TransportMode.access_walk)
                || leg.getMode().equals(TransportMode.egress_walk) || leg.getMode().equals(TransportMode.walk) ){
            tmpScore += - Math.exp(travelTime * modeParams.marginalUtilityOfTraveling_s) + 1;
        }else{
            tmpScore += travelTime * modeParams.marginalUtilityOfTraveling_s;
        }

        if (modeParams.marginalUtilityOfDistance_m != 0.0
                || modeParams.monetaryDistanceCostRate != 0.0) {
            Route route = leg.getRoute();
            double dist = route.getDistance(); // distance in meters
            if ( Double.isNaN(dist) ) {
                if ( ccc<10 ) {
                    ccc++ ;
                    Logger.getLogger(this.getClass()).warn("distance is NaN. Will make score of this plan NaN. Possible reason: Simulation does not report " +
                            "a distance for this trip. Possible reason for that: mode is teleported and router does not " +
                            "write distance into plan.  Needs to be fixed or these plans will die out.") ;
                    if ( ccc==10 ) {
                        Logger.getLogger(this.getClass()).warn(Gbl.FUTURE_SUPPRESSED) ;
                    }
                }
            }
            tmpScore += modeParams.marginalUtilityOfDistance_m * dist;
            tmpScore += modeParams.monetaryDistanceCostRate * this.params.marginalUtilityOfMoney * dist;
        }
        tmpScore += modeParams.constant;
        // (yyyy once we have multiple legs without "real" activities in between, this will produce wrong results.  kai, dec'12)
        // (yy NOTE: the constant is added for _every_ pt leg.  This is not how such models are estimated.  kai, nov'12)
        return tmpScore;
    }

    public static void closeBW() {
        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void closeIteration(){
        iteration++;
        boardingTime.clear();
    }
}
