package masterThesis.drt.data;

import masterThesis.drt.optimizer.VehicleData;
import org.matsim.contrib.dynagent.DynAgent;

import java.util.ArrayList;

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
