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

import masterThesis.drt.data.DrtRequest;
import masterThesis.drt.optimizer.VehicleData;
import masterThesis.drt.optimizer.insertion.filter.DrtVehicleFilter;
import masterThesis.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import masterThesis.dvrp.data.Vehicle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author michalm
 */
public class ParallelMultiVehicleInsertionProblem {

	private static class TaskGroup {
		private final List<VehicleData.Entry> vEntries = new ArrayList<VehicleData.Entry>();
		private final MultiVehicleInsertionProblem multiInsertionProblem;

		private TaskGroup(SingleVehicleInsertionProblem singleInsertionProblem) {
			this.multiInsertionProblem = new MultiVehicleInsertionProblem(singleInsertionProblem);
		}

		private BestInsertion findBestInsertion(DrtRequest drtRequest) {
			BestInsertion bestInsertion = multiInsertionProblem.findBestInsertion(drtRequest, vEntries);
			vEntries.clear();
			return bestInsertion;
		}

		private BestInsertion singleInsertion(DrtRequest drtRequest, VehicleData vdata){
			BestInsertion bestInsertion = multiInsertionProblem.singleVehicleInsertion(drtRequest, vdata);
			return bestInsertion;
		}
	}

	private final int threads;
	private final TaskGroup[] taskGroups;
	private final ExecutorService executorService;
	private final DrtVehicleFilter filter;

	public ParallelMultiVehicleInsertionProblem(SingleVehicleInsertionProblem[] singleInsertionProblems, DrtVehicleFilter filter) {
		threads = singleInsertionProblems.length;
		this.filter = filter;
		this.taskGroups = new TaskGroup[threads];
		for (int i = 0; i < threads; i++) {
			taskGroups[i] = new TaskGroup(singleInsertionProblems[i]);
		}
		executorService = Executors.newFixedThreadPool(threads);
	}


	public BestInsertion findBestInsertion(DrtRequest drtRequest, VehicleData vData) {
		List<VehicleData.Entry> filteredVehicles = filter.applyFilter(drtRequest, vData);
		divideTasksIntoGroups(filteredVehicles);
		return findBestInsertion(submitTasks(drtRequest));
	}

	// find random thread to add tasks... It should work
	public BestInsertion insertionSingleVehicleProblem(DrtRequest req, VehicleData veh) {
		int randomIdx = ThreadLocalRandom.current().nextInt(0, threads);
		return this.taskGroups[randomIdx].singleInsertion(req, veh);
	}

	
	private void divideTasksIntoGroups(List<VehicleData.Entry> filteredVehicles) {
		Iterator<VehicleData.Entry> vEntryIter = filteredVehicles.iterator();
		int div = filteredVehicles.size() / threads;
		int mod = filteredVehicles.size() % threads;

		for (int i = 0; i < threads; i++) {
			int count = div + (i < mod ? 1 : 0);
			for (int j = 0; j < count; j++) {
				taskGroups[i].vEntries.add(vEntryIter.next());
			}
		}

		if (vEntryIter.hasNext()) {
			throw new RuntimeException();
		}
	}

	private List<Future<BestInsertion>> submitTasks(final DrtRequest drtRequest) {
		List<Future<BestInsertion>> bestInsertionFutures = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			final TaskGroup taskGroup = taskGroups[i];
			bestInsertionFutures.add(executorService.submit(new Callable<BestInsertion>() {
				public BestInsertion call() {
					return taskGroup.findBestInsertion(drtRequest);
				}
			}));
		}

		return bestInsertionFutures;
	}

	private BestInsertion findBestInsertion(List<Future<BestInsertion>> bestInsertionFutures) {
		double minCost = Double.MAX_VALUE;
		BestInsertion fleetBestInsertion = null;
		for (Future<BestInsertion> bestInsertionFuture : bestInsertionFutures) {
			try {
				BestInsertion bestInsertion = bestInsertionFuture.get();
				if (bestInsertion != null && bestInsertion.cost < minCost) {
					fleetBestInsertion = bestInsertion;
					minCost = bestInsertion.cost;
				}
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
		return fleetBestInsertion;
	}

	public void shutdown() {
		executorService.shutdown();
	}
}
