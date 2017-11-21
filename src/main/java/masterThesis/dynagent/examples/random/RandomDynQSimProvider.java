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

package masterThesis.dynagent.examples.random;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;

import java.util.Collection;

public class RandomDynQSimProvider implements Provider<Mobsim> {
	private final Scenario scenario;
	private final EventsManager events;
	private final Collection<AbstractQSimPlugin> plugins;

	@Inject
	public RandomDynQSimProvider(Scenario scenario, EventsManager events, Collection<AbstractQSimPlugin> plugins) {
		this.scenario = scenario;
		this.events = events;
		this.plugins = plugins;
	}

	@Override
	public Mobsim get() {
		QSim qSim = QSimUtils.createQSim(scenario, events, plugins);
		qSim.addAgentSource(new RandomDynAgentSource(qSim, 100));
		return qSim;
	}
}
