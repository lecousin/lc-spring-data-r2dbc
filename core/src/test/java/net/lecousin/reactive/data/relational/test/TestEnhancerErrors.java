package net.lecousin.reactive.data.relational.test;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalInitializer;
import net.lecousin.reactive.data.relational.enhance.Enhancer;
import net.lecousin.reactive.data.relational.model.ModelAccessException;

public class TestEnhancerErrors {
	
	@BeforeAll
	public static void init() {
		LcReactiveDataRelationalInitializer.init();
	}

	@Test
	public void testEnhanceAgain() {
		Enhancer.enhance(Arrays.asList(Enhancer.getEntities().iterator().next().getName()));
	}
	
	@Test
	public void testEnhanceNonEntityClass() {
		try {
			Enhancer.enhance(Arrays.asList(TestEnhancerErrors.class.getName()));
			throw new AssertionError("Error expected");
		} catch (ModelAccessException e) {
			// ok
		}
	}
	
}
