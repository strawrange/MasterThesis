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

package masterThesis.drt.optimizer.insertion;

import masterThesis.drt.data.DrtGenerator;
import masterThesis.drt.data.DrtRequest;
import masterThesis.drt.data.OnDemandDrtGenerator;
import masterThesis.drt.data.OnDemandDrtKiller;
import masterThesis.drt.optimizer.AbstractDrtOptimizer;
import masterThesis.drt.optimizer.DrtOptimizerContext;
import masterThesis.drt.optimizer.VehicleData;
import masterThesis.drt.passenger.events.DrtRequestRejectedEvent;
import masterThesis.drt.passenger.events.DrtRequestScheduledEvent;
import masterThesis.drt.run.DrtConfigGroup;
import masterThesis.dvrp.data.DrtRequests;
import masterThesis.dvrp.vrpagent.VrpAgentSource;
import org.apache.log4j.Logger;
import masterThesis.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import masterThesis.router.BackwardFastMultiNodeDijkstra;
import masterThesis.router.InverseArrayRoutingNetworkFactory;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.ArrayFastRouterDelegateFactory;
import org.matsim.core.router.FastMultiNodeDijkstra;
import org.matsim.core.router.FastMultiNodeDijkstraFactory;
import org.matsim.core.router.FastRouterDelegateFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.utils.misc.Time;

import java.util.*;

/**
 * @author michalm
 */
public class InsertionDrtOptimizer extends AbstractDrtOptimizer implements MobsimBeforeCleanupListener {
	private final ParallelMultiVehicleInsertionProblem insertionProblem;
	private final EventsManager eventsManager;
	private final boolean printWarnings;
	private DrtGenerator generator;
	private OnDemandDrtKiller killer = new OnDemandDrtKiller(getOptimContext());

	public InsertionDrtOptimizer(DrtOptimizerContext optimContext, DrtConfigGroup drtCfg) {
		super(optimContext, new PriorityQueue<DrtRequest>(DrtRequests.UPDATE_TIME_COMPARATOR));
		this.eventsManager = optimContext.qSim.getEventsManager();
		printWarnings = drtCfg.isPrintDetailedWarnings();

		// TODO bug: cannot cast ImaginaryNode to RoutingNetworkNode
		// PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		// preProcessDijkstra.run(optimContext.network);
		PreProcessDijkstra preProcessDijkstra = null;
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();

		RoutingNetwork inverseRoutingNetwork = new InverseArrayRoutingNetworkFactory(preProcessDijkstra)
				.createRoutingNetwork(optimContext.network);

		SingleVehicleInsertionProblem[] singleVehicleInsertionProblems = new SingleVehicleInsertionProblem[drtCfg
				.getNumberOfThreads()];
		for (int i = 0; i < singleVehicleInsertionProblems.length; i++) {
			FastMultiNodeDijkstraFactory routerFactory = new FastMultiNodeDijkstraFactory(true);
			FastMultiNodeDijkstra router = (FastMultiNodeDijkstra) routerFactory.createPathCalculator(optimContext.network, optimContext.travelDisutility,
					optimContext.travelTime);
			BackwardFastMultiNodeDijkstra backwardRouter = new BackwardFastMultiNodeDijkstra(inverseRoutingNetwork,
					optimContext.travelDisutility, optimContext.travelTime, preProcessDijkstra, fastRouterFactory,
					true);
			singleVehicleInsertionProblems[i] = new SingleVehicleInsertionProblem(router, backwardRouter,
					optimContext.scheduler.getParams().stopDurationConstant + optimContext.scheduler.getParams().stopDurationBeta, drtCfg.getMaxWaitTime(), optimContext.qSim.getSimTimer(), drtCfg.getDetourIdx());
		}

		insertionProblem = new ParallelMultiVehicleInsertionProblem(singleVehicleInsertionProblems,optimContext.filter);
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		insertionProblem.shutdown();
	}


	@Override
	protected void scheduleUnplannedRequests() {
		if (getUnplannedRequests().isEmpty()) {
			return;
		}
		killer.vehicleKiller();
		VehicleData vData = new VehicleData(getOptimContext(), getOptimContext().fleet.getVehicles().values());

		Iterator<DrtRequest> reqIter = getUnplannedRequests().iterator();
		ArrayList<DrtRequest> newRequests = new ArrayList<>();
		while (reqIter.hasNext()) {
			DrtRequest req = reqIter.next();
 			if (req.getUpdateTime()  != getOptimContext().qSim.getSimTimer().getTimeOfDay()){
                getUnplannedRequests().addAll(newRequests);
				return;
			}
			BestInsertion best;
			if(req.getPassenger().getMode().equals(DrtConfigGroup.DRT_CREATION)) {
 				VehicleData singleVData = generator.generateDrtAgent(req);
				best = insertionProblem.insertionSingleVehicleProblem(req, singleVData);
				vData.addEntry(best.vehicleEntry);
			}else{
				best = insertionProblem.findBestInsertion(req, vData);
				if (best == null) {
					eventsManager
							.processEvent(new DrtRequestRejectedEvent(getOptimContext().qSim.getSimTimer().getTimeOfDay(), req.getId(), req.getPassenger().getId()));
					if (printWarnings) {
						Logger.getLogger(getClass()).warn("No vehicle found for drt request from passenger \t"
								+ req.getPassenger().getId() + "\tat\t" + Time.writeTime(req.getSubmissionTime()));
					}
					if (getOptimContext().qSim.getSimTimer().getTimeOfDay() - req.getSubmissionTime() <= getOptimContext().drtConfig.getAbortTime()) {
						double nextUpdatedTime = getOptimContext().qSim.getSimTimer().getTimeOfDay() + getOptimContext().drtConfig.getRequestUpdateTime();
                        req.setUpdateTime(nextUpdatedTime);
                        newRequests.add(req);
                    }else{
					    eventsManager.processEvent(new PersonStuckEvent(getOptimContext().qSim.getSimTimer().getTimeOfDay(),req.getPassenger().getId(),req.getFromLink().getId(),req.getPassenger().getMode()));
                    }
				}
			}
			if (best != null) {
                getOptimContext().scheduler.insertRequest(best.vehicleEntry, req, best.insertion);
                vData.updateEntry(best.vehicleEntry);
                eventsManager.processEvent(new DrtRequestScheduledEvent(getOptimContext().qSim.getSimTimer().getTimeOfDay(),
                        req.getId(), best.vehicleEntry.vehicle.getId(), req.getPickupTask().getEndTime(),
                        req.getDropoffTask().getBeginTime()));
			}
            reqIter.remove();
		}
		getUnplannedRequests().addAll(newRequests);
	}

	public void getVrp(VrpAgentSource vrpAgentSource) {
		this.generator = new OnDemandDrtGenerator(getOptimContext(),vrpAgentSource);
	}
}
