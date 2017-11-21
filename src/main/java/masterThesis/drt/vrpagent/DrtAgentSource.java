package masterThesis.drt.vrpagent;

import com.google.inject.Inject;
import masterThesis.dvrp.data.Fleet;
import masterThesis.dvrp.optimizer.VrpOptimizer;
import masterThesis.dvrp.vrpagent.VrpAgentLogic;
import masterThesis.dvrp.vrpagent.VrpAgentSource;
import org.matsim.core.mobsim.qsim.QSim;

public class DrtAgentSource extends VrpAgentSource {
    @Inject
    public DrtAgentSource(VrpAgentLogic.DynActionCreator nextActionCreator, Fleet fleet, VrpOptimizer optimizer, QSim qSim) {
        super(nextActionCreator, fleet, optimizer, qSim);
    }
}
