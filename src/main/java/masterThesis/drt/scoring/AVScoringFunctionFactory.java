package masterThesis.drt.scoring;

import ch.ethz.matsim.av.config.AVConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import masterThesis.drt.run.DrtConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;

@Singleton
public class AVScoringFunctionFactory implements ScoringFunctionFactory {
    protected Network network;
    private final ScoringParametersForPerson params;

    @Inject
    public AVScoringFunctionFactory(Scenario scenario, DrtConfigGroup config) {
        this.network = scenario.getNetwork();
        this.params = new SubpopulationScoringParameters(scenario);
    }

    public ScoringFunction createNewScoringFunction(Person person) {

        final ScoringParameters parameters = params.getScoringParameters( person );

        SumScoringFunction sumScoringFunction = new SumScoringFunction();
        sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring( parameters ));
        sumScoringFunction.addScoringFunction(new AVScoringFunction(this.params.getScoringParameters(person),this.network));
        sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring( parameters ));
        sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring( parameters ));
        return sumScoringFunction;

    }
}
