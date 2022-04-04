package net.lecousin.reactive.data.relational.tests;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.MappingException;

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

}
