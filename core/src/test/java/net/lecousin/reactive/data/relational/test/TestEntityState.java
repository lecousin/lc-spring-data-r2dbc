package net.lecousin.reactive.data.relational.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.LcReactiveDataRelationalInitializer;
import net.lecousin.reactive.data.relational.enhance.EntityState;

public class TestEntityState {

	@BeforeAll
	public static void init() {
		LcReactiveDataRelationalInitializer.init();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testGetStateWithoutEntityType() {
		TestEntity entity = new TestEntity();
		LcReactiveDataRelationalClient client = Mockito.mock(LcReactiveDataRelationalClient.class);
		MappingContext mappingContext = Mockito.mock(MappingContext.class);
		RelationalPersistentEntity entityType = Mockito.mock(RelationalPersistentEntity.class);
		Mockito.when(client.getMappingContext()).thenReturn(mappingContext);
		Mockito.when(mappingContext.getRequiredPersistentEntity(TestEntity.class)).thenReturn(entityType);
		EntityState.get(entity, client);
	}
	
	@Test
	public void testGetEntityStateError() {
		TestEntity entity = new TestEntity();
		try {
			EntityState.get(entity, null);
		} catch (Exception e) {
			// ok
		}
	}
}
