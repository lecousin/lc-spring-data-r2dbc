package net.lecousin.reactive.data.relational.test;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.enhance.Enhancer;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;

@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
public abstract class AbstractBasicTest extends AbstractLcReactiveDataRelationalTest {

	@Test
	public void testEnhanceAgain() {
		Enhancer.enhance(Arrays.asList(Enhancer.getEntities().iterator().next().getName()));
	}
	
	@Test
	public void testEnhanceNonEntityClass() {
		try {
			Enhancer.enhance(Arrays.asList(getClass().getName()));
			throw new AssertionError("Error expected");
		} catch (ModelAccessException e) {
			// ok
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testGetStateWithoutEntityType() throws Exception {
		Entity entity = new Entity();
		LcReactiveDataRelationalClient client = Mockito.mock(LcReactiveDataRelationalClient.class);
		MappingContext mappingContext = Mockito.mock(MappingContext.class);
		RelationalPersistentEntity entityType = Mockito.mock(RelationalPersistentEntity.class);
		Mockito.when(client.getMappingContext()).thenReturn(mappingContext);
		Mockito.when(mappingContext.getRequiredPersistentEntity(entity.getClass())).thenReturn(entityType);
		EntityState.get(entity, client);
	}
	
	@Test
	public void testGetEntityStateError() throws Exception {
		Entity entity = new Entity();
		try {
			EntityState.get(entity, null);
		} catch (Exception e) {
			// ok
		}
	}

}
