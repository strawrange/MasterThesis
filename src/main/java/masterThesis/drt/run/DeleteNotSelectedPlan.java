package masterThesis.drt.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

import java.util.Iterator;

public class DeleteNotSelectedPlan {

    public static void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile("/home/biyu/IdeaProjects/MasterThesis/scenarios/siouxFalls/population_10prct.xml.gz");
        Population population = scenario.getPopulation();
        for (Person p: population.getPersons().values()) {
            Plan selectedPlan = p.getSelectedPlan();
            Iterator<PlanElement> planIter = selectedPlan.getPlanElements().iterator();
            boolean pt = false;
            while (planIter.hasNext()) {
                PlanElement planElement = planIter.next();
                if (planElement instanceof Leg) {
                    if (((Leg) planElement).getMode().equals(TransportMode.car) || ((Leg) planElement).getMode().equals(TransportMode.walk)) {
                        ((Leg) planElement).setMode(DrtConfigGroup.DRT_MODE);
                        ((Leg) planElement).setRoute(null);
                    }
                    if (((Leg) planElement).getMode().equals(TransportMode.transit_walk)) {
                        planIter.remove();
                    }
                    if (((Leg) planElement).getMode().equals(TransportMode.pt)) {
                        if (!pt){
                            ((Leg) planElement).setMode(DrtConfigGroup.DRT_MODE);
                            ((Leg) planElement).setRoute(null);
                            pt = true;
                        }else{
                            planIter.remove();
                        }

                    }
                }
                if (planElement instanceof Activity) {
                    if (((Activity) planElement).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
                        planIter.remove();
                    }else{
                        pt = false;
                    }
                }
            }
        }
        new PopulationWriter(scenario.getPopulation()).write("new_plans2.xml.gz");
    }
}
