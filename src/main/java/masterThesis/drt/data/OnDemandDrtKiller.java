package masterThesis.drt.data;

import masterThesis.drt.optimizer.DrtOptimizerContext;
import masterThesis.drt.schedule.DrtStayTask;
import masterThesis.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.api.core.v01.Id;

import java.util.Iterator;
import java.util.Map;

public class OnDemandDrtKiller {
    DrtOptimizerContext optimizerContext;

    public OnDemandDrtKiller(DrtOptimizerContext optimContext) {
        this.optimizerContext = optimContext;
    }

    public void vehicleKiller(){
        for (Iterator< Map.Entry<Id<Vehicle>, Vehicle>> vehIter = ((FleetImpl)optimizerContext.fleet).getModifiableVehicles().entrySet().iterator(); vehIter.hasNext();){
            Map.Entry<Id<Vehicle>,Vehicle> vehicleEntry = vehIter.next();
            double idleTime = optimizerContext.qSim.getSimTimer().getTimeOfDay() - vehicleEntry.getValue().getSchedule().getCurrentTask().getBeginTime();
            if (vehicleEntry.getValue().getSchedule().getCurrentTask() instanceof DrtStayTask && idleTime > optimizerContext.drtConfig.getKillingTime()){
                ((DrtStayTask) vehicleEntry.getValue().getSchedule().getCurrentTask()).finish(optimizerContext.qSim.getSimTimer().getTimeOfDay());
                vehIter.remove();
            }
        }
    }
}
