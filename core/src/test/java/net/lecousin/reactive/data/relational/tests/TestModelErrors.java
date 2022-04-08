package net.lecousin.reactive.data.relational.tests;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.relational.core.mapping.Column;

import net.lecousin.reactive.data.relational.annotations.CompositeId;
import net.lecousin.reactive.data.relational.annotations.JoinTable;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelException;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.model.metadata.EntityStaticMetadata;

class TestModelErrors {

	public static class NonEnhancedEntity {
		@SuppressWarnings("unused")
		private EntityState _lcState;
	}
	
	@Test
	void test() throws Exception {
		Assertions.assertThrows(ModelAccessException.class, () -> EntityStaticMetadata.get(getClass()));
		Assertions.assertThrows(ModelException.class, () -> EntityStaticMetadata.setClasses(Arrays.asList(getClass())));
		
		EntityStaticMetadata.setClasses(Arrays.asList(NonEnhancedEntity.class));
		EntityStaticMetadata ti = EntityStaticMetadata.get(NonEnhancedEntity.class);
		Assertions.assertNull(ti.getJoinTableElementsForJoinTableClass("test", getClass()));
		
		Assertions.assertNull(ModelUtils.getAsCollection(this));
		Assertions.assertNull(ModelUtils.getCollectionType(NonEnhancedEntity.class.getDeclaredField("_lcState")));
		Assertions.assertThrows(MappingException.class, () -> ModelUtils.getRequiredCollectionType(NonEnhancedEntity.class.getDeclaredField("_lcState")));
	}
	
	@CompositeId(properties = { "hello", "world" }, indexName = "test")
	public static class InvalidCompositeId {
		@SuppressWarnings("unused")
		private EntityState _lcState;

		@Column
		private String text;
	}
	
	@Test
	void testInvalidCompositeId() throws Exception {
		try {
			EntityStaticMetadata.setClasses(Arrays.asList(InvalidCompositeId.class));
			throw new AssertionError();
		} catch (ModelAccessException e) {
			Assertions.assertTrue(e.getMessage().contains("hello does not exist"));
		}
	}

	
	public static class JoinFrom {
		@SuppressWarnings("unused")
		private EntityState _lcState;

		@JoinTable
		private JoinTo join;
	}
	
	public static class JoinTo {
		@SuppressWarnings("unused")
		private EntityState _lcState;
	}

	@Test
	void testInvalidJoinTable() throws Exception {
		try {
			EntityStaticMetadata.setClasses(Arrays.asList(JoinFrom.class, JoinTo.class));
			throw new AssertionError();
		} catch (ModelAccessException e) {
			// ok
		}
	}
}
