package masterThesis.drt.postsimulation;

import masterThesis.drt.run.DrtConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

import java.util.Iterator;
import java.util.Random;

public class TenPercentDRTCreation {

    public static void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile("/home/biyu/IdeaProjects/MasterThesis/scenarios/siouxFalls/population_10prct_drt_new.xml.gz");
        Population population = scenario.getPopulation();
        for (Person p: population.getPersons().values()) {
            for (PlanElement planElement: p.getSelectedPlan().getPlanElements()){
                Random random = new Random();
                if (planElement instanceof Leg){
                    if (random.nextInt(10) == 0){
                        ((Leg) planElement).setMode(DrtConfigGroup.DRT_CREATION);
                        break;
                    }
                }
            }
        }
        new PopulationWriter(scenario.getPopulation()).write("/home/biyu/IdeaProjects/MasterThesis/scenarios/siouxFalls/population_10prct_90drt_new.xml.gz");
    }
}
