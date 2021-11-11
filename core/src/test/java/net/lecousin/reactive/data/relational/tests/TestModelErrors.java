package net.lecousin.reactive.data.relational.tests;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.MappingException;

import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.LcEntityTypeInfo;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelException;
import net.lecousin.reactive.data.relational.model.ModelUtils;

class TestModelErrors {

	public static class NonEnhancedEntity {
		private EntityState _lcState;
	}
	
	@Test
	void test() throws Exception {
		Assertions.assertThrows(ModelAccessException.class, () -> LcEntityTypeInfo.get(getClass()));
		Assertions.assertThrows(ModelException.class, () -> LcEntityTypeInfo.setClasses(Arrays.asList(getClass())));
		
		LcEntityTypeInfo.setClasses(Arrays.asList(NonEnhancedEntity.class));
		LcEntityTypeInfo ti = LcEntityTypeInfo.get(NonEnhancedEntity.class);
		Assertions.assertThrows(MappingException.class, () -> ti.getRequiredForeignTableFieldForJoinKey("test", getClass()));
		Assertions.assertThrows(MappingException.class, () -> ti.getRequiredForeignTableFieldForProperty("test"));
		Assertions.assertThrows(MappingException.class, () -> ti.getRequiredForeignTableForProperty("test"));
		Assertions.assertNull(ti.getForeignTableFieldForJoinKey("test", getClass()));
		Assertions.assertNull(ti.getForeignTableFieldForProperty("test"));
		Assertions.assertNull(ti.getForeignTableForProperty("test"));
		Assertions.assertNull(ti.getJoinTable("test"));
		Assertions.assertNull(ti.getJoinTableElementsForJoinTableClass("test", getClass()));
		
		Assertions.assertNull(ModelUtils.getAsCollection(this));
		Assertions.assertNull(ModelUtils.getCollectionType(NonEnhancedEntity.class.getDeclaredField("_lcState")));
		Assertions.assertThrows(MappingException.class, () -> ModelUtils.getRequiredCollectionType(NonEnhancedEntity.class.getDeclaredField("_lcState")));
	}

}
