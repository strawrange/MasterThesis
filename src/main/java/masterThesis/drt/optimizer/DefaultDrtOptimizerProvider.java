/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package masterThesis.drt.optimizer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import masterThesis.drt.optimizer.insertion.InsertionDrtOptimizer;
import masterThesis.drt.optimizer.insertion.InsertionDrtOptimizerWithDepots;
import masterThesis.drt.optimizer.insertion.filter.DrtVehicleFilter;
import masterThesis.drt.optimizer.insertion.filter.KNearestVehicleFilter;
import masterThesis.drt.optimizer.insertion.filter.NoFilter;
import masterThesis.drt.run.DrtConfigGroup;
import masterThesis.drt.scheduler.DrtScheduler;
import masterThesis.drt.scheduler.DrtSchedulerParams;
import masterThesis.dvrp.data.FleetImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import masterThesis.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class DefaultDrtOptimizerProvider implements Provider<DrtOptimizer> {
	public static final String DRT_OPTIMIZER = "drt_optimizer";

	private final DrtConfigGroup drtCfg;
	private final Network network;
	private final FleetImpl fleet;
	private final TravelTime travelTime;
	private final QSim qSim;



	@Inject(optional = true)
	private @Named(DRT_OPTIMIZER) TravelDisutilityFactory travelDisutilityFactory;

	@Inject
	public DefaultDrtOptimizerProvider(DrtConfigGroup drtCfg, @Named(DvrpModule.DVRP_ROUTING) Network network,
									   Fleet fleet, @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime, QSim qSim) {
		this.drtCfg = drtCfg;
		this.network = network;
		this.fleet = (FleetImpl) fleet;
		this.travelTime = travelTime;
		this.qSim = qSim;
		if (!this.fleet.getVehicles().isEmpty()){
		    this.fleet.initialize();
        }
	}

	@Override
	public DrtOptimizer get() {
		DrtSchedulerParams schedulerParams = new DrtSchedulerParams(drtCfg.getStopDurationConstant(),drtCfg.getStopDurationBeta());
		DrtScheduler scheduler = new DrtScheduler(drtCfg, fleet, qSim.getSimTimer(), schedulerParams, travelTime);

//		DrtVehicleFilter filter = new DistanceFilter(drtCfg.getEstimatedDrtSpeed()*drtCfg.getMaxWaitTime()*drtCfg.getEstimatedBeelineDistanceFactor());
//		DrtVehicleFilter filter = new KNearestVehicleFilter(14);
		DrtVehicleFilter filter = null;
		if (drtCfg.getkNearestVehicles()>0){
			filter = new KNearestVehicleFilter(drtCfg.getkNearestVehicles());
		}
		else {
			filter = new NoFilter();
		}
		TravelDisutility travelDisutility = travelDisutilityFactory == null ? new TimeAsTravelDisutility(travelTime)
				: travelDisutilityFactory.createTravelDisutility(travelTime);

		DrtOptimizerContext optimContext = new DrtOptimizerContext(fleet, network, qSim, travelTime,
				travelDisutility, scheduler, filter, drtCfg);

		return drtCfg.getIdleVehiclesReturnToDepots() ? new InsertionDrtOptimizerWithDepots(optimContext, drtCfg)
				: new InsertionDrtOptimizer(optimContext, drtCfg);
	}
}
