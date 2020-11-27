package net.lecousin.reactive.data.relational.test;

import org.junit.jupiter.engine.config.CachingJupiterConfiguration;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalInitializer;

public class LcReactiveDataRelationalTestEngine implements TestEngine {

	@Override
	public String getId() {
		return "net.lecousin.reactive.data.relational.engine";
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		LcReactiveDataRelationalInitializer.init();
		JupiterConfiguration configuration = new CachingJupiterConfiguration(new DefaultJupiterConfiguration(discoveryRequest.getConfigurationParameters()));
		return new JupiterEngineDescriptor(uniqueId, configuration);
	}

	@Override
	public void execute(ExecutionRequest request) {
		// do nothing
	}


}
