package masterThesis.drt.optimizer;

import masterThesis.drt.optimizer.insertion.filter.DrtVehicleFilter;
import masterThesis.drt.run.DrtConfigGroup;
import masterThesis.drt.scheduler.DrtScheduler;
import org.matsim.api.core.v01.network.Network;
import masterThesis.dvrp.data.Fleet;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;


/**
 * @author michalm
 */
public class DrtOptimizerContext {
	public final Fleet fleet;
	public final Network network;
	public final TravelTime travelTime;
	public final TravelDisutility travelDisutility;
	public final DrtScheduler scheduler;
	public final QSim qSim;
	public final DrtVehicleFilter filter;
	public final DrtConfigGroup drtConfig;

	public DrtOptimizerContext(Fleet fleet, Network network, QSim qSim, TravelTime travelTime,
							   TravelDisutility travelDisutility, DrtScheduler scheduler, DrtVehicleFilter filter, DrtConfigGroup drtconfig) {
		this.fleet = fleet;
		this.network = network;
		this.qSim = qSim;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.scheduler = scheduler;
		this.filter = filter;
		this.drtConfig = drtconfig;
	}
}
