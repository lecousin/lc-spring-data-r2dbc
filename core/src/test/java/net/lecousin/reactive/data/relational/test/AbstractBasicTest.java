package net.lecousin.reactive.data.relational.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.enhance.Enhancer;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.ModelException;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;
import net.lecousin.reactive.data.relational.schema.Column;
import net.lecousin.reactive.data.relational.schema.RelationalDatabaseSchema;
import net.lecousin.reactive.data.relational.schema.SchemaBuilderFromEntities;
import net.lecousin.reactive.data.relational.schema.SchemaException;
import net.lecousin.reactive.data.relational.schema.Table;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;
import net.lecousin.reactive.data.relational.schema.dialect.SchemaStatement;
import net.lecousin.reactive.data.relational.schema.dialect.SchemaStatements;

@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
public abstract class AbstractBasicTest extends AbstractLcReactiveDataRelationalTest {

	@Override
	protected Collection<Class<?>> usedEntities() {
		return new LinkedList<>();
	}
	
	protected abstract Class<? extends RelationalDatabaseSchemaDialect> expectedDialect();
	
	@Test
	public void testDialect() {
		Assertions.assertEquals(expectedDialect(), lcClient.getSchemaDialect().getClass());
		Assertions.assertEquals(expectedDialect(), RelationalDatabaseSchemaDialect.getDialect(lcClient.getDialect()).getClass());
		Assertions.assertTrue(lcClient.getSchemaDialect().isCompatible(lcClient.getDialect()));
	}
	
	@Test
	public void testEnhanceAgain() throws Exception {
		Collection<Class<?>> entities = getAllCompatibleEntities();
		Enhancer.enhance(Arrays.asList(entities.iterator().next().getName()));
		Enhancer.enhance(Arrays.asList(entities.iterator().next().getName()));
	}
	
	@Test
	public void testEnhanceNonEntityClass() {
		try {
			Enhancer.enhance(Arrays.asList(getClass().getName()));
			throw new AssertionError("Error expected");
		} catch (ModelException e) {
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
	
	@Test
	public void testPrintSchema() {
		RelationalDatabaseSchema schema = new SchemaBuilderFromEntities(lcClient).build(getAllCompatibleEntities());
		lcClient.getSchemaDialect().createSchemaContent(schema).print(System.out);
	}
	
	@Test
	public void testSchema() {
		RelationalDatabaseSchema schema = new SchemaBuilderFromEntities(lcClient).build(getAllCompatibleEntities());
		try {
			schema.getTable("Toto");
			throw new AssertionError();
		} catch (NoSuchElementException e) {
			// ok
		}
		Table table = schema.getTable("basic");
		table.getColumn("str");
		try {
			table.getColumn("toto");
			throw new AssertionError();
		} catch (NoSuchElementException e) {
			// ok
		}
	}
	
	@Test
	public void testInvalidSchemaStatement() {
		SchemaStatement statement = new SchemaStatement("THIS IS INVALID");
		SchemaStatements statements = new SchemaStatements();
		statements.add(statement);
		try {
			statements.execute(lcClient).block();
			throw new AssertionError();
		} catch (Exception e) {
			// ok
		}
	}
	
	@Test
	public void testSchemaDialect() {
		RelationalDatabaseSchema schema = new SchemaBuilderFromEntities(lcClient).build(getAllCompatibleEntities());
		Table table = schema.getTable("basic");
		Column col = table.getColumn("str");
		try {
			lcClient.getSchemaDialect().getColumnType(col, getClass(), null);
			throw new AssertionError();
		} catch (SchemaException e) {
			// ok
		}
	}

}
