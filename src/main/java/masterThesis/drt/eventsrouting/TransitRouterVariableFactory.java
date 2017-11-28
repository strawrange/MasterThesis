package masterThesis.drt.eventsrouting;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.RoutingModule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;


public class TransitRouterVariableFactory implements Provider<RoutingModule> {

    @Inject
    private TransitRouterConfig config;
    @Inject
    private TransitRouterNetworkTravelTimeAndDisutility ttCalculator;
    @Inject
    private TransitRouterNetworkWW transitNetwork;
    @Inject
    PlanCalcScoreConfigGroup scoreConfig;

    private final String mode;


    public TransitRouterVariableFactory(String mode){
        this.mode = mode;
    }

    @Override
    public RoutingModule get() {
        return new TransitRouterVariableImpl(config, ttCalculator, transitNetwork, mode, scoreConfig);
    }
}
