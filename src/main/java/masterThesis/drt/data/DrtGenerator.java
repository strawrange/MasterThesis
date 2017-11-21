package masterThesis.drt.data;

import masterThesis.drt.optimizer.DrtOptimizerContext;
import masterThesis.drt.optimizer.VehicleData;
import masterThesis.dvrp.vrpagent.VrpAgentSource;
import masterThesis.dynagent.DynAgent;
import sun.security.jca.GetInstance;

import java.util.ArrayList;
import java.util.List;

import static masterThesis.drt.data.OnDemandDrtGenerator.newDynAgents;

public interface DrtGenerator {

   VehicleData generateDrtAgent(DrtRequest req);
   static ArrayList<DynAgent> getNewDynAgents(){
       ArrayList<DynAgent> newAgents = new ArrayList<>();
       newAgents.addAll(newDynAgents);
       newDynAgents.clear();
       return newAgents;
   }

}
