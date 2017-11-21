package masterThesis.drt.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.vehicles.Vehicle;

public class AVRecord {
    private final Id<Leg> legId;
    private final Id<Vehicle> vehicleId;
    private final double legStartTime;
    private final double personEnterVehicleTime;
    private double legEndTime;
    private double avgOccupancy;
    private double maxOccupancy;
    private double waitingScore;
    private double travellingScore;
    private double bonus1;
    private double bonus2;
    private double finalScore1;
    private double finalScore2;

    public AVRecord(Id<Leg> legId, PersonEntersVehicleEvent event,double legStartTime, double utilityW){
        this.legId = legId;
        this.vehicleId = event.getVehicleId();
        this.legStartTime = legStartTime;
        this.personEnterVehicleTime = event.getTime();
        this.waitingScore = (personEnterVehicleTime - legStartTime) *(utilityW);
    }

    public void calculatePersonLeavesVehicleEvent(double legEndTime,double avgOccupancy,double maxOccupancy,double bonus1, double utility, double finalScore2){
        this.legEndTime = legEndTime;
        this.avgOccupancy = avgOccupancy;
        this.maxOccupancy = maxOccupancy;
        this.bonus1 = bonus1;
        this.travellingScore = utility * (legEndTime - personEnterVehicleTime);
        this.bonus2 = -utility * (legEndTime - personEnterVehicleTime) * (avgOccupancy-1) / avgOccupancy;
        this.finalScore1 =  this.waitingScore + travellingScore + bonus1;
        this.finalScore2 = (legEndTime-legStartTime)*utility  + finalScore2;
    }

    public Id<Leg> getLegId() {
        return legId;
    }

    public Id<Vehicle> getVehicleId() {
        return vehicleId;
    }

    public double getLegStartTime() {
        return legStartTime;
    }


    public double getLegEndTime() {
        return legEndTime;
    }


    public double getAvgOccupancy() {
        return avgOccupancy;
    }


    public double getMaxOccupancy() {
        return maxOccupancy;
    }


    public double getWaitingScore() {
        return waitingScore;
    }

    public double getTravellingScore() {
        return travellingScore;
    }


    public double getBonus1() {
        return bonus1;
    }


    public double getBonus2() {
        return bonus2;
    }

    public double getFinalScore1() {
        return finalScore1;
    }

    public double getFinalScore2() {
        return finalScore2;
    }

}
