package masterThesis.dvrp.passenger;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import masterThesis.dvrp.optimizer.VrpOptimizer;
import masterThesis.dvrp.run.DvrpModule;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.ArrayList;
import java.util.Collection;

public class PassengerEnginePlugin extends AbstractQSimPlugin {
	private final String mode;

	public PassengerEnginePlugin(Config config, String mode) {
		super(config);
		this.mode = mode;
	}

	@Override
	public Collection<? extends Module> modules() {
		Collection<Module> result = new ArrayList<>();
		result.add(new AbstractModule() {
			@Override
			public void configure() {
				bind(PassengerEngine.class).toProvider(new PassengerEngineProvider(mode)).asEagerSingleton();
			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends DepartureHandler>> departureHandlers() {
		Collection<Class<? extends DepartureHandler>> result = new ArrayList<>();
		result.add(PassengerEngine.class);
		return result;
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		Collection<Class<? extends MobsimEngine>> result = new ArrayList<>();
		result.add(PassengerEngine.class);
		return result;
	}

	public static class PassengerEngineProvider implements Provider<PassengerEngine> {
		private final String mode;

		@Inject
		private EventsManager eventsManager;
		@Inject
		private PassengerRequestCreator requestCreator;
		@Inject
		private VrpOptimizer optimizer;
		@Inject
		@Named(DvrpModule.DVRP_ROUTING)
		private Network network;

		public PassengerEngineProvider(String mode) {
			this.mode = mode;
		}

		@Override
		public PassengerEngine get() {
			return new PassengerEngine(mode, eventsManager, requestCreator, optimizer, network);
		}
	}
}
