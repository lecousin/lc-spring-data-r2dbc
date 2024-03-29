package net.lecousin.reactive.data.relational.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.enhance.Enhancer;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelException;
import net.lecousin.reactive.data.relational.model.metadata.EntityMetadata;
import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.SqlQuery;
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
		try {
			Enhancer.enhance(Arrays.asList(entities.iterator().next().getName()));
			throw new AssertionError();
		} catch (Throwable t) {
			// expected
		}
		try {
			Enhancer.enhance(Arrays.asList(entities.iterator().next().getName()));
			throw new AssertionError();
		} catch (Throwable t) {
			// expected
		}
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
	
	@Test
	public void testGetEntityStateError() throws Exception {
		EntityWithTransientFields entity = new EntityWithTransientFields();
		try {
			EntityState.get(entity, (EntityMetadata) null);
		} catch (Exception e) {
			// ok
		}
		try {
			EntityState.get(entity, (LcReactiveDataRelationalClient) null);
		} catch (Exception e) {
			// ok
		}
	}
	
	@Test
	public void testEntityTransientFields() throws Exception {
		RelationalDatabaseSchema schema = lcClient.buildSchemaFromEntities(Arrays.asList(EntityWithTransientFields.class));
		lcClient.dropCreateSchemaContent(schema).block();
		
		EntityWithTransientFields entity = new EntityWithTransientFields();
		entity.setStr("aTest");
		entity.setTextNotSaved("should be null");
		entity.setDefaultHello("world");
		lcClient.save(entity).block();
		
		List<EntityWithTransientFields> list = SelectQuery.from(EntityWithTransientFields.class, "e").execute(lcClient).collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("aTest", entity.getStr());
		Assertions.assertNull(entity.getTextNotSaved());
		Assertions.assertNull(entity.getDefaultHello());
		Assertions.assertNull(entity.getClient());
		
		lcClient.delete(entity).block();

		list = SelectQuery.from(EntityWithTransientFields.class, "e").execute(lcClient).collectList().block();
		Assertions.assertEquals(0, list.size());
	}
	
	@Test
	public void testEntityWithNonNullableProperty() {
		RelationalDatabaseSchema schema = lcClient.buildSchemaFromEntities(Arrays.asList(EntityWithNonNullProperty.class));
		lcClient.dropCreateSchemaContent(schema).block();
		
		EntityWithNonNullProperty entity1 = new EntityWithNonNullProperty();
		Assertions.assertThrows(DataIntegrityViolationException.class, () -> lcClient.save(entity1).block());
		entity1.setNonNullable(Boolean.TRUE);
		lcClient.save(entity1).block();
				
		EntityWithNonNullProperty entity2 = SelectQuery.from(EntityWithNonNullProperty.class, "e").execute(lcClient).blockFirst();
		Assertions.assertEquals(Boolean.TRUE, entity2.getNonNullable());
		entity2.setNonNullable(null);
		Assertions.assertThrows(DataIntegrityViolationException.class, () -> lcClient.save(entity2).block());
	}
	
	@Test
	public void testPrintSchema() {
		RelationalDatabaseSchema schema = SchemaBuilderFromEntities.build(lcClient.getEntities(getAllCompatibleEntities()));
		lcClient.getSchemaDialect().createSchemaContent(schema).print(System.out);
	}
	
	@Test
	public void testSchema() {
		RelationalDatabaseSchema schema = SchemaBuilderFromEntities.build(lcClient.getEntities(getAllCompatibleEntities()));
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
		RelationalDatabaseSchema schema = SchemaBuilderFromEntities.build(lcClient.getEntities(getAllCompatibleEntities()));
		Table table = schema.getTable("basic");
		Column col = table.getColumn("str");
		try {
			lcClient.getSchemaDialect().getColumnType(col, getClass(), null);
			throw new AssertionError();
		} catch (SchemaException e) {
			// ok
		}
	}
	
	@Test
	public void testInvalidQuery() {
		SqlQuery<Integer> q = new SqlQuery<>(lcClient);
		q.setQuery(1);
		Assertions.assertEquals(1, q.getQuery());
		try {
			q.execute().fetch().all().collectList().block();
			throw new AssertionError();
		} catch (IllegalArgumentException e) {
			// ok
		}
	}
	
	@Test
	public void testGetNonEntity() {
		Assertions.assertThrows(ModelAccessException.class, () -> lcClient.getRequiredEntity(String.class));
	}
	
	@Test
	public void testSaveAndDeleteNonEntity() {
		Assertions.assertThrows(ModelAccessException.class, () -> lcClient.save("hello").block());
		Assertions.assertThrows(ModelAccessException.class, () -> lcClient.save(Arrays.asList("invalid")).blockFirst());
		lcClient.save(Arrays.asList()).blockFirst(); // empty => no error
		lcClient.saveAll(Arrays.asList()).block(); // empty => no error
		Assertions.assertThrows(ModelAccessException.class, () -> lcClient.delete("hello").block());
		Assertions.assertThrows(ModelAccessException.class, () -> lcClient.delete(Arrays.asList("invalid")).block());
		lcClient.delete(Arrays.asList()).block(); // empty => no error
	}

}
