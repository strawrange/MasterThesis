package masterThesis.drt.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Test {
    public static void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(args[0]);
        int car = 0, drt = 0;
        int car2 = 0, drt2 = 0;
        MainModeIdentifier identifier = new MainModeIdentifierImpl();
        for (Person person : scenario.getPopulation().getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            for (PlanElement element : plan.getPlanElements())
                if (element instanceof Leg) {
                    if (((Leg) element).getMode().equals("car"))
                        car++;
                    else if (((Leg) element).getMode().equals("drt"))
                        drt++;
                }
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan, new StageActivityTypesImpl(new String[]{"pt interaction","drt interaction"}));
            for(TripStructureUtils.Trip trip:trips) {
                String mode = identifier.identifyMainMode(trip.getTripElements());
                if (mode.equals("car"))
                    car2++;
                else if (mode.equals("drt"))
                    drt2++;
            }

        }
        Map<String, Double> modeCnt = new TreeMap<>();
        for (Person person : scenario.getPopulation().getPersons().values()) {
            Plan plan = person.getSelectedPlan() ;
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan, new StageActivityTypesImpl(new String[]{"pt interaction","drt interaction"})) ;
            for ( TripStructureUtils.Trip trip : trips ) {
                String mode = identifier.identifyMainMode( trip.getTripElements() ) ;
                // yy as stated elsewhere, the "computer science" mode identification may not be the same as the "transport planning"
                // mode identification.  Maybe revise.  kai, nov'16

                Double cnt = modeCnt.get( mode );
                if ( cnt==null ) {
                    cnt = 0. ;
                }
                modeCnt.put( mode, cnt + 1 ) ;
            }
        }
        System.out.println("Car " + car);
        System.out.println("Drt " + drt);
        System.out.println("Car " + car2);
        System.out.println("Drt " + drt2);
        System.out.println("Car " + modeCnt.get("car"));
        System.out.println("Drt " + modeCnt.get("drt"));
        System.out.println("PT " + modeCnt.get("pt"));
        System.out.println("Walk " + modeCnt.get("walk"));
    }

}
