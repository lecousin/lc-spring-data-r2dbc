package net.lecousin.reactive.data.relational.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalInitializer;
import net.lecousin.reactive.data.relational.LcReactiveDataRelationalInitializer.Config;
import net.lecousin.reactive.data.relational.enhance.ClassPathScanningEntities;

class TestClasspathScanning {

	@Test
	public void test() throws Exception {
		Set<String> classes = ClassPathScanningEntities.scan();
		Config config = LcReactiveDataRelationalInitializer.loadConfiguration();
		ArrayList<String> fromScan = new ArrayList<>(classes);
		Collections.sort(fromScan);
		List<String> fromYaml = config.getEntities();
		Collections.sort(fromYaml);
		
		// fake entities for test
		fromScan.remove("net.lecousin.reactive.data.relational.tests.TestEnhancerErrors$LoadedEntity");
		fromScan.remove("net.lecousin.reactive.data.relational.tests.TestEnhancerErrors$JoinFrom");
		fromScan.remove("net.lecousin.reactive.data.relational.tests.TestEnhancerErrors$JoinTo");
		fromScan.remove("net.lecousin.reactive.data.relational.tests.invalid.InvalidJoinTable1");
		fromScan.remove("net.lecousin.reactive.data.relational.tests.invalid.InvalidJoinTable2");
		
		System.out.println("from scan = " + fromScan);
		System.out.println("from yaml = " + fromYaml);
		Assertions.assertIterableEquals(fromYaml, fromScan);
	}
	
}
