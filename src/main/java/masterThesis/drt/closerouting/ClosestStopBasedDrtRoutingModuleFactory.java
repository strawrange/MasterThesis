package masterThesis.drt.closerouting;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import masterThesis.drt.run.DrtConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.RoutingModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class ClosestStopBasedDrtRoutingModuleFactory implements Provider<RoutingModule> {
    private String mode;
    @Inject
    @Named(TransportMode.walk)
    private RoutingModule walkRouter;
    @Inject
    @Named(TransportMode.access_walk)
    private RoutingModule accessWalkRouter;
    @Inject
    @Named(DrtConfigGroup.GROUP_NAME)
    private TransitSchedule transitSchedule;
    @Inject
    private Scenario scenario;
    public ClosestStopBasedDrtRoutingModuleFactory (String mode){
     this.mode = mode;
    }

    @Override
    public RoutingModule get() {
        return new ClosestStopBasedDrtRoutingModule(walkRouter, accessWalkRouter, transitSchedule, scenario, mode);
    }
}
