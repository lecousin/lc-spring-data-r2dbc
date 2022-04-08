package net.lecousin.reactive.data.relational.tests;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.mapping.Table;

import net.lecousin.reactive.data.relational.annotations.JoinTable;
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
	
	@Table
	public static class JoinFrom {
		@JoinTable
		private JoinTo join;
	}
	
	@Table
	public static class JoinTo {
		
	}
	
	@Test
	void testInvalidJoinTable_NotaSet() {
		try {
			Enhancer.enhance(Arrays.asList("net.lecousin.reactive.data.relational.tests.invalid.InvalidJoinTable1", "net.lecousin.reactive.data.relational.tests.invalid.InvalidJoinTable1"));
			throw new AssertionError();
		} catch (ModelException e) {
			Assertions.assertTrue(e.getMessage().contains("must be a Set"));
		}
	}
	
	@Test
	void testInvalidJoinTable_InvalidLazyGetter() {
		try {
			Enhancer.enhance(Arrays.asList("net.lecousin.reactive.data.relational.tests.invalid.InvalidJoinTable3", "net.lecousin.reactive.data.relational.tests.invalid.InvalidJoinTable4"));
			throw new AssertionError();
		} catch (Throwable e) {
			// ok
		}
	}
	
	@Test
	void testInvalidJoinTable_InvalidJoinSetter() {
		try {
			Enhancer.enhance(Arrays.asList("net.lecousin.reactive.data.relational.tests.invalid.InvalidJoinTable5", "net.lecousin.reactive.data.relational.tests.invalid.InvalidJoinTable6"));
			throw new AssertionError();
		} catch (Exception e) {
			// ok
		}
	}

}
