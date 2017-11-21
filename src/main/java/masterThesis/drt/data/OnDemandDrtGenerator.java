package masterThesis.drt.data;

import masterThesis.drt.optimizer.DrtOptimizerContext;
import masterThesis.drt.optimizer.VehicleData;
import masterThesis.drt.schedule.DrtStayTask;
import masterThesis.dvrp.data.FleetImpl;
import masterThesis.dvrp.data.Vehicle;
import masterThesis.dvrp.data.VehicleImpl;
import masterThesis.dvrp.vrpagent.VrpAgentSource;
import masterThesis.dynagent.DynAgent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

public class OnDemandDrtGenerator implements DrtGenerator {
    protected static final ArrayList<DynAgent> newDynAgents = new ArrayList<>();

    private DrtOptimizerContext optimContext;
    private VrpAgentSource vrpAgentSource;
    private ArrayList<Id<Vehicle>> vehicleIDList = new ArrayList<>();

    public OnDemandDrtGenerator(DrtOptimizerContext optimContext, VrpAgentSource vrpAgentSource) {
        this.optimContext = optimContext;
        this.vrpAgentSource = vrpAgentSource;
        vehicleIDList.clear();
    }

    private Vehicle addVehicle(DrtRequest req){
        int i = 0;
        Link startLink = req.getFromLink();
        Id<Vehicle> id = Id.create("AV" + i + "__" + req.getPassenger().getId(), Vehicle.class);
        while (vehicleIDList.contains(id)){
            i++;
            id = Id.create("AV" + i + "__" + req.getPassenger().getId(), Vehicle.class);
        }
        vehicleIDList.add(id);
        // after 1s the vehicle begin to serve.
        // the way to get service end time is not very clean... Maybe there is a better method...
        double endTime = optimContext.fleet.getServiceEndTime();
        Vehicle veh = new VehicleImpl(id,startLink,optimContext.drtConfig.getCapacity(),optimContext.qSim.getSimTimer().getTimeOfDay()-1,endTime);
        return veh;
    }

    @Override
    public VehicleData generateDrtAgent(DrtRequest req) {
        Vehicle veh = addVehicle(req);
        DynAgent agent = vrpAgentSource.updateAgentsInMobsim(veh);
        veh.getSchedule()
                .addTask(new DrtStayTask(veh.getServiceBeginTime(), veh.getServiceEndTime(), veh.getStartLink()));
        optimContext.fleet.addVehicle(veh);
        newDynAgents.add(agent);
        FleetImpl newFleet = new FleetImpl(optimContext.fleet.getServiceEndTime());
        newFleet.addVehicle(veh);
        agent.endActivityAndComputeNextState(optimContext.qSim.getSimTimer().getTimeOfDay());
        VehicleData singleVData = new VehicleData(optimContext, newFleet.getVehicles().values());
        return singleVData;
    }

}
