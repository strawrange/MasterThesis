package masterThesis.dvrp.vrpagent;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;

import java.util.ArrayList;
import java.util.Collection;

public class VrpAgentSourcePlugin extends AbstractQSimPlugin {
	public VrpAgentSourcePlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		Collection<Module> result = new ArrayList<>();
		result.add(new AbstractModule() {
			@Override
			public void configure() {
				bind(VrpAgentSource.class).asEagerSingleton();
			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends AgentSource>> agentSources() {
		Collection<Class<? extends AgentSource>> result = new ArrayList<>();
		result.add(VrpAgentSource.class);
		return result;
	}
}
