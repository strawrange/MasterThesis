package masterThesis.drt.postsimulation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class WalkTimeAnalysis {
    private static int ITER = 40;
    private static String FOLDER = "/home/biyu/IdeaProjects/MasterThesis/output/drt_10_10prct_two_modes_creation_penalty30_changeSingleTripMode_alwaysCreate_0.1modeChange_vehicleKill_abortTime_transitRouter_linearWalkScore_creationRoutingWait0_annealing_drtDisutility4/ITERS/it." + ITER + "/";
    public static void main(String[] args) throws IOException {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(FOLDER + ITER + ".experienced_plans.xml.gz");
        Population population = scenario.getPopulation();
        walkTimeAnalysis(population);
    }

    private static void walkTimeAnalysis(Population population) throws IOException {
        BufferedWriter trips =  IOUtils.getBufferedWriter(FOLDER + "walkAnalysis.csv");
        for (Person person:population.getPersons().values()){
            for (PlanElement planElement: person.getSelectedPlan().getPlanElements()){
                if (planElement instanceof Leg){
                    if (((Leg) planElement).getMode().equals(TransportMode.walk)){
                        trips.write(person.getId() + ";" +String.valueOf(((Leg) planElement).getTravelTime()));
                        trips.newLine();
                    }
                }
            }
        }
        trips.close();
    }
}
