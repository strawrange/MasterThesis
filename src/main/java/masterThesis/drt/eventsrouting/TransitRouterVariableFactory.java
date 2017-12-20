package masterThesis.drt.eventsrouting;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import masterThesis.drt.run.DrtConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.RoutingModule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;


public class TransitRouterVariableFactory implements Provider<RoutingModule> {

    @Inject
    private DrtTransitRouterConfig config;
    @Inject
    private TransitRouterNetworkWW transitNetwork;
    @Inject
    private TransitRouterCommonDisutility delegate;


    private final String mode;

    public TransitRouterVariableFactory(String mode){
        this.mode = mode;
    }

    @Override
    public RoutingModule get() {

        return new TransitRouterVariableImpl(config, transitNetwork, mode, delegate);
    }
}
