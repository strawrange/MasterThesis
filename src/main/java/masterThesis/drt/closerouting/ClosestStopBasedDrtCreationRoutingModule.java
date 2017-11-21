package masterThesis.drt.closerouting;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import masterThesis.drt.run.DrtConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.RoutingModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class ClosestStopBasedDrtCreationRoutingModule extends ClosestStopBasedDrtRoutingModule {
    /**
     * @param walkRouter
     * @param accessWalkRouter
     * @param transitSchedule
     * @param scenario
     */
    @Inject
    public ClosestStopBasedDrtCreationRoutingModule(@Named(TransportMode.walk) RoutingModule walkRouter,
                                            @Named(TransportMode.access_walk) RoutingModule accessWalkRouter,
                                            @Named(DrtConfigGroup.DRT_MODE) TransitSchedule transitSchedule, Scenario scenario) {
        super(walkRouter, accessWalkRouter, transitSchedule, scenario);
    }

    @Override
    protected String identifyMode(){
        return DrtConfigGroup.DRT_CREATION;
    }
}
