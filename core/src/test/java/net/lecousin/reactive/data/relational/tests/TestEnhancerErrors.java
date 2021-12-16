package net.lecousin.reactive.data.relational.tests;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.enhance.Enhancer;
import net.lecousin.reactive.data.relational.model.ModelException;

class TestEnhancerErrors {

	@Table
	public static class LoadedEntity {
		
	}
	
	@Test
	void testEnhanceLoadedClass() {
		String name = LoadedEntity.class.getName();
		Assertions.assertThrows(Throwable.class, () -> Enhancer.enhance(Arrays.asList(name)));
		Assertions.assertThrows(ModelException.class, () -> Enhancer.enhance(Arrays.asList("do.not.exist")));
	}
	
}
