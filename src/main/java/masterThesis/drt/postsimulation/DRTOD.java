package masterThesis.drt.postsimulation;

import masterThesis.drt.closerouting.DrtStageActivityType;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesFromPopulation;
import org.matsim.facilities.MatsimFacilitiesReader;
import sun.nio.ch.IOUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

public class DRTOD {
    private static int ITER = 100;
    private static String FOLDER = "C:/Users/wangb/git/MasterThesis/output/walkScoreLinear800_withAnnealing_requestUpdate600_noRideSharingBonus_detourAlpha1.5_detourBeta600_newDRTConstant30_2/ITERS/it." + ITER + "/";
    private static String PLANFILE =  FOLDER + ITER + ".experienced_plans.xml.gz";
    private static String FACILITYFILE = "C:/Users/wangb/git/MasterThesis/scenarios/siouxFalls/Siouxfalls_facilities.xml";

    public static void main(String[] args) throws IOException {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(PLANFILE);
        Population population = scenario.getPopulation();
        new MatsimFacilitiesReader(scenario).readFile(FACILITYFILE);
        Map<Id<ActivityFacility>, ? extends ActivityFacility> facilities = scenario.getActivityFacilities().getFacilities();
        writeDRTOD(population, facilities);
    }

    private static void writeDRTOD(Population population, Map<Id<ActivityFacility>, ? extends ActivityFacility> facilities) throws IOException {
        BufferedWriter bw = IOUtils.getBufferedWriter(FOLDER + "drtOD.csv");
        bw.write("actId;personId;actType;actX;actY;actStartTime;actEndTime");
        for (Person person : population.getPersons().values()){
            int i = 0;
            for (PlanElement planElement:person.getSelectedPlan().getPlanElements()){
                if (planElement instanceof Activity){
                    if (((Activity) planElement).getType().equals(DrtStageActivityType.DRTSTAGEACTIVITY)){
                        continue;
                    }
                    bw.newLine();
                    Coord coord = facilities.get(((Activity) planElement).getFacilityId()).getCoord();
                    bw.write( i++ + ";" + person.getId() + ";" + ((Activity) planElement).getType() + ";" +coord.getX() +
                            ";" + coord.getY() + ";" + ((Activity) planElement).getStartTime() + ";" +
                            ((Activity) planElement).getEndTime());
                }
            }
        }
        bw.close();
    }

}


