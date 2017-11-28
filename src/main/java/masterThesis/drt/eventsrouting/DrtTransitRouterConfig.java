package masterThesis.drt.eventsrouting;

import com.google.inject.Inject;
import masterThesis.drt.run.DrtConfigGroup;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.pt.router.TransitRouterConfig;

public class DrtTransitRouterConfig extends TransitRouterConfig {

    private final double marginalUtilityOfTravelTimeDrt_utl_s;

    private final double marginalUtilityOfTravelDistanceDrt_utl_m;

    private final double performing_utils_s;

    @Inject
    public DrtTransitRouterConfig(Config config) {
        super(config);
        // add drt score function in...
        this.marginalUtilityOfTravelTimeDrt_utl_s = config.planCalcScore().getModes().get(DrtConfigGroup.DRT_MODE).getMarginalUtilityOfTraveling() /3600.0;

        this.marginalUtilityOfTravelDistanceDrt_utl_m = config.planCalcScore().getMarginalUtilityOfMoney() *
                config.planCalcScore().getModes().get(DrtConfigGroup.DRT_MODE).getMonetaryDistanceRate() +
                config.planCalcScore().getModes().get(DrtConfigGroup.DRT_MODE).getMarginalUtilityOfDistance();

        this.performing_utils_s = config.planCalcScore().getPerforming_utils_hr()/3600.;
    }

    public double getMarginalUtilityOfTravelTimeDrt_utl_s() {
        return marginalUtilityOfTravelTimeDrt_utl_s;
    }

    public double getMarginalUtilityOfWaitingDrt_utl_s() {
        return getMarginalUtilityOfWaitingPt_utl_s() + performing_utils_s;
    }

    public double getMarginalUtilityOfTravelDistanceDrt_utl_m() {
        return marginalUtilityOfTravelDistanceDrt_utl_m;
    }
    @Override
    public double getMarginalUtilityOfTravelTimeWalk_utl_s() {
        return super.getMarginalUtilityOfTravelTimeWalk_utl_s() + performing_utils_s;
    }
    @Override
    public double getMarginalUtilityOfTravelTimePt_utl_s() {
        return getMarginalUtilityOfTravelTimePt_utl_s() + performing_utils_s;
    }
}
