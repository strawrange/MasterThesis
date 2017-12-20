/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package masterThesis.drt.run;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import masterThesis.drt.analysis.DrtAnalysisModule;
import masterThesis.drt.analysis.DrtScoringAnalysis;
import masterThesis.drt.closerouting.ClosestStopBasedDrtRoutingModuleFactory;
import masterThesis.drt.eventsrouting.*;
import masterThesis.drt.eventsrouting.stopstoptime.StopStopTime;
import masterThesis.drt.eventsrouting.stopstoptime.StopStopTimeCalculator;
import masterThesis.drt.eventsrouting.waitstoptime.WaitTime;
import masterThesis.drt.eventsrouting.waitstoptime.WaitTimeCalculator;
import masterThesis.drt.optimizer.DefaultDrtOptimizerProvider;
import masterThesis.drt.optimizer.DrtOptimizer;
import masterThesis.drt.passenger.DrtRequestCreator;
import masterThesis.drt.closerouting.DrtRoutingModule;
import masterThesis.drt.closerouting.DrtStageActivityType;
import masterThesis.drt.scoring.AVScoringFunctionFactory;
import masterThesis.drt.vrpagent.DrtActionCreator;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import masterThesis.dvrp.data.Fleet;
import masterThesis.dvrp.data.FleetImpl;
import masterThesis.dvrp.data.Vehicle;
import masterThesis.dvrp.data.VehicleImpl;
import masterThesis.dvrp.data.file.VehicleReader;
import masterThesis.dvrp.optimizer.VrpOptimizer;
import masterThesis.dvrp.passenger.PassengerRequestCreator;
import masterThesis.dvrp.run.DvrpConfigConsistencyChecker;
import masterThesis.dvrp.run.DvrpModule;
import masterThesis.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author jbischoff
 *
 */
public class DrtControlerCreator {

	public static Controler createControler(Config config, boolean otfvis) {
		DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
		config.addConfigConsistencyChecker(new DvrpConfigConsistencyChecker());
		config.checkConsistency();
		if (drtCfg.getOperationalScheme().equals(DrtConfigGroup.OperationalScheme.stationbased) || drtCfg.getOperationalScheme().equals(DrtConfigGroup.OperationalScheme.stationbasedclose)){
			ActivityParams params = config.planCalcScore().getActivityParams(DrtStageActivityType.DRTSTAGEACTIVITY);
			if (params == null)
			{
				// keep parameters in line with pt_interaction
				params = new ActivityParams(DrtStageActivityType.DRTSTAGEACTIVITY);
				params.setTypicalDuration(120);//1
				params.setOpeningTime(0.) ;//null
				params.setClosingTime(0.) ;//null
				params.setScoringThisActivityAtAll(false);
				config.planCalcScore().addActivityParams(params);
				Logger.getLogger(DrtControlerCreator.class).info("drt interaction scoring parameters not set. Adding default values (activity will not be scored).");
			}
		}
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(
				new DvrpModule(createModuleForQSimPlugin(DefaultDrtOptimizerProvider.class), DrtOptimizer.class) {
					@Provides
					@Singleton
					// input vehicles from file or generate vehicles in the system
					private Fleet provideVehicles(@Named(DvrpModule.DVRP_ROUTING) Network network, Config config,
							DrtConfigGroup drtCfg) {
						FleetImpl fleet = new FleetImpl(config.qsim().getEndTime());
						if (drtCfg.isInputVehicleFile()) {
							new VehicleReader(network, fleet).parse(drtCfg.getVehiclesFileUrl(config.getContext()));
						}else{
							for(int i = 0; i < drtCfg.getInitialFleetSize(); i++){
								int startLinkIdx = ThreadLocalRandom.current().nextInt(0, network.getLinks().size());
								Link startLink = (new ArrayList<Link>(network.getLinks().values())).get(startLinkIdx);
								Id<Vehicle> id = Id.create("taxibus" + i, Vehicle.class);
								Vehicle veh = new VehicleImpl(id,startLink,drtCfg.getCapacity(),0,config.qsim().getEndTime());
								fleet.addVehicle(veh);
							}
						}
						return fleet;
					}

				});
		controler.addOverridingModule(new DrtAnalysisModule());

		switch (drtCfg.getOperationalScheme()) {
			case door2door: {
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						addRoutingModuleBinding(DrtConfigGroup.DRT_MODE).to(DrtRoutingModule.class);
						bind(ScoringFunctionFactory.class).to(AVScoringFunctionFactory.class).asEagerSingleton();
					}
				});
				break;
			}
			case stationbasedclose: {
				final Scenario scenario2 = ScenarioUtils.createScenario(config);
				new TransitScheduleReader(scenario2)
						.readFile(drtCfg.getTransitStopsFileUrl(config.getContext()).getFile());

				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bind(TransitSchedule.class).annotatedWith(Names.named(DrtConfigGroup.DRT_MODE))
								.toInstance(scenario2.getTransitSchedule());
						addRoutingModuleBinding(DrtConfigGroup.DRT_MODE).toProvider(new ClosestStopBasedDrtRoutingModuleFactory(DrtConfigGroup.DRT_MODE));
						addRoutingModuleBinding(DrtConfigGroup.DRT_CREATION).toProvider(new ClosestStopBasedDrtRoutingModuleFactory(DrtConfigGroup.DRT_CREATION));
						bind(ScoringFunctionFactory.class).to(AVScoringFunctionFactory.class).asEagerSingleton();
						addControlerListenerBinding().to(DrtScoringAnalysis.class);
					}
				});
				break;
			}
			case stationbased: {
				final Scenario scenario2 = ScenarioUtils.createScenario(config);
				new TransitScheduleReader(scenario2)
						.readFile(drtCfg.getTransitStopsFileUrl(config.getContext()).getFile());
                TransitRouterNetworkWW routerNetwork = TransitRouterNetworkWW.createFromStops(scenario2.getTransitSchedule());
                PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
                preProcessDijkstra.run(routerNetwork);
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bind(ScoringFunctionFactory.class).to(AVScoringFunctionFactory.class).asEagerSingleton();
						bind(TransitSchedule.class).annotatedWith(Names.named(DrtConfigGroup.DRT_MODE))
								.toInstance(scenario2.getTransitSchedule());
						bind(TransitRouterConfig.class).to(DrtTransitRouterConfig.class);
						bind(DrtTransitRouterConfig.class).asEagerSingleton();
						bind(WaitTime.class).toProvider(WaitTimeCalculator.class).asEagerSingleton();
						bind(StopStopTime.class).toProvider(StopStopTimeCalculator.class).asEagerSingleton();
						bind(TransitRouterNetworkWW.class).toInstance(routerNetwork);
						bind(TransitRouterCommonDisutility.class).asEagerSingleton();
						addControlerListenerBinding().to(TransitRouterCommonDisutility.class);
						addRoutingModuleBinding(DrtConfigGroup.DRT_MODE).toProvider(new TransitRouterVariableFactory(DrtConfigGroup.DRT_MODE));
						addRoutingModuleBinding(DrtConfigGroup.DRT_CREATION).toProvider(new TransitRouterVariableFactory(DrtConfigGroup.DRT_CREATION));
						addControlerListenerBinding().to(DrtScoringAnalysis.class);
					}
				});
				break;
			}
			default:
				throw new IllegalStateException();
		}
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		return controler;
	}

	private static com.google.inject.AbstractModule createModuleForQSimPlugin(
			final Class<? extends Provider<? extends DrtOptimizer>> providerClass) {
		return new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(DrtOptimizer.class).toProvider(providerClass).asEagerSingleton();
				bind(VrpOptimizer.class).to(DrtOptimizer.class);
				bind(DynActionCreator.class).to(DrtActionCreator.class).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(DrtRequestCreator.class).asEagerSingleton();
			}
		};
	}
}
