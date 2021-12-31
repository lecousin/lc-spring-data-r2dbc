package net.lecousin.reactive.data.relational.test.onetoonemodel;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;
import net.lecousin.reactive.data.relational.test.AbstractLcReactiveDataRelationalTest;
import reactor.core.publisher.Flux;

@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
public abstract class AbstractTestOneToOneModel extends AbstractLcReactiveDataRelationalTest {

	@Autowired
	private MyEntity1Repository repo1;
	
	@Autowired
	private MyEntity2Repository repo2;
	
	@Autowired
	private MyEntity3Repository repo3;
	
	@Override
	protected Collection<Class<?>> usedEntities() {
		return Arrays.asList(MyEntity1.class, MyEntity2.class, MyEntity3.class, MySubEntity1.class, MySubEntity2.class, MySubEntity3.class);
	}
	
	@Test
	public void testEntity1WithoutSubEntity() {
		MyEntity1 entity = new MyEntity1();
		entity.setValue("test");
		repo1.save(entity).block();
		
		List<MyEntity1> list = repo1.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test", entity.getValue());
		Assertions.assertNull(entity.getSubEntity());
		
		entity.setValue("modified");
		repo1.save(entity).block();
		list = repo1.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("modified", entity.getValue());
		Assertions.assertNull(entity.getSubEntity());
		Assertions.assertNull(entity.lazyGetSubEntity().block());
		Assertions.assertNull(entity.lazyGetSubEntity().block());
		
		repo1.deleteAll(list).block();
		Assertions.assertEquals(0, repo1.findAll().collectList().block().size());
	}
	
	@Test
	public void testEntity1WithSubEntity() {
		MyEntity1 entity = new MyEntity1();
		entity.setValue("test");
		MySubEntity1 subEntity = new MySubEntity1();
		subEntity.setSubValue("sub test");
		entity.setSubEntity(subEntity);
		subEntity.setParent(entity);
		repo1.save(entity).block();
		
		List<MyEntity1> list = repo1.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test", entity.getValue());
		Assertions.assertNull(entity.getSubEntity());
		subEntity = entity.lazyGetSubEntity().block();
		Assertions.assertEquals("sub test", subEntity.getSubValue());
		Assertions.assertEquals("sub test", entity.getSubEntity().getSubValue());
		Assertions.assertTrue(subEntity == entity.lazyGetSubEntity().block());
		
		list = repo1.findByValue("abcd").collectList().block();
		Assertions.assertEquals(0, list.size());
		
		list = repo1.findByValue("test").collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test", entity.getValue());
		Assertions.assertNotNull(entity.getSubEntity());
		Assertions.assertEquals("sub test", entity.getSubEntity().getSubValue());
		
		// update entity value
		entity.setValue("test 2");
		repo1.save(entity).block();
		list = repo1.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test 2", entity.getValue());
		subEntity = entity.lazyGetSubEntity().block();
		Assertions.assertEquals("sub test", subEntity.getSubValue());
		Assertions.assertEquals("sub test", entity.getSubEntity().getSubValue());
		
		// update sub entity value, save parent entity
		subEntity.setSubValue("sub test 2");
		repo1.save(entity).block();
		list = repo1.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test 2", entity.getValue());
		subEntity = entity.lazyGetSubEntity().block();
		Assertions.assertEquals("sub test 2", subEntity.getSubValue());
		Assertions.assertEquals("sub test 2", entity.getSubEntity().getSubValue());
		Assertions.assertEquals(1, SelectQuery.from(MyEntity1.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(1, SelectQuery.from(MySubEntity1.class, "entity").execute(lcClient).collectList().block().size());
		
		// change sub entity
		subEntity = new MySubEntity1();
		subEntity.setSubValue("new one");
		entity.setSubEntity(subEntity);
		repo1.save(entity).block();
		list = repo1.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test 2", entity.getValue());
		subEntity = entity.lazyGetSubEntity().block();
		Assertions.assertEquals("new one", subEntity.getSubValue());
		Assertions.assertEquals("new one", entity.getSubEntity().getSubValue());
		Assertions.assertEquals(1, SelectQuery.from(MyEntity1.class, "entity").execute(lcClient).collectList().block().size());
		// the old one must be removed
		Assertions.assertEquals(1, SelectQuery.from(MySubEntity1.class, "entity").execute(lcClient).collectList().block().size());
		
		repo1.delete(entity).block();
		Assertions.assertEquals(0, SelectQuery.from(MyEntity1.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(0, SelectQuery.from(MySubEntity1.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testEntity2WithoutSubEntity() {
		MyEntity2 entity = new MyEntity2();
		entity.setValue("test");
		repo2.save(entity).block();
		
		List<MyEntity2> list = repo2.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test", entity.getValue());
		Assertions.assertNull(entity.getSubEntity());
		
		entity.setValue("modified");
		repo2.save(entity).block();
		list = repo2.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("modified", entity.getValue());
		Assertions.assertNull(entity.getSubEntity());

		repo2.deleteAll(list).block();
		Assertions.assertEquals(0, repo2.findAll().collectList().block().size());
	}
	
	@Test
	public void testEntity2WithSubEntity() {
		MyEntity2 entity = new MyEntity2();
		entity.setValue("test");
		MySubEntity2 subEntity = new MySubEntity2();
		subEntity.setSubValue("sub test");
		entity.setSubEntity(subEntity);
		subEntity.setParent(entity);
		repo2.save(entity).block();
		
		List<MyEntity2> list = repo2.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test", entity.getValue());
		Assertions.assertNotNull(entity.getSubEntity());
		Assertions.assertEquals(entity, entity.getSubEntity().getParent());
		Assertions.assertFalse(entity.getSubEntity().entityLoaded());
		entity.getSubEntity().loadEntity().block();
		Assertions.assertEquals("sub test", entity.getSubEntity().getSubValue());
		
		list = repo2.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertNull(entity.getSubEntity().getSubValue()); // not yet loaded
		Assertions.assertEquals("sub test", entity.lazyGetSubEntity().block().getSubValue());
		
		list = repo2.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertNull(entity.getSubEntity().getSubValue()); // not yet loaded
		Assertions.assertEquals("sub test", entity.getSubEntity().lazyGetSubValue().block());
		
		
		// update entity value
		entity.setValue("test 2");
		repo2.save(entity).block();
		list = repo2.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test 2", entity.getValue());
		Assertions.assertNotNull(entity.getSubEntity());
		Assertions.assertEquals(entity, entity.getSubEntity().getParent());
		Assertions.assertFalse(entity.getSubEntity().entityLoaded());
		entity.getSubEntity().loadEntity().block();
		Assertions.assertEquals("sub test", entity.getSubEntity().getSubValue());
		
		// update sub entity value, save parent entity
		entity.getSubEntity().setSubValue("sub test 2");
		repo2.save(entity).block();
		list = repo2.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test 2", entity.getValue());
		Assertions.assertNotNull(entity.getSubEntity());
		Assertions.assertEquals(entity, entity.getSubEntity().getParent());
		Assertions.assertFalse(entity.getSubEntity().entityLoaded());
		entity.getSubEntity().loadEntity().block();
		Assertions.assertEquals("sub test 2", entity.getSubEntity().getSubValue());
		Assertions.assertEquals(1, SelectQuery.from(MyEntity2.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(1, SelectQuery.from(MySubEntity2.class, "entity").execute(lcClient).collectList().block().size());
		
		// change sub entity
		subEntity = new MySubEntity2();
		subEntity.setSubValue("new one");
		entity.setSubEntity(subEntity);
		repo2.save(entity).block();
		list = repo2.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test 2", entity.getValue());
		Assertions.assertNotNull(entity.getSubEntity());
		Assertions.assertEquals(entity, entity.getSubEntity().getParent());
		Assertions.assertFalse(entity.getSubEntity().entityLoaded());
		entity.getSubEntity().loadEntity().block();
		Assertions.assertEquals("new one", entity.getSubEntity().getSubValue());
		Assertions.assertEquals(1, SelectQuery.from(MyEntity2.class, "entity").execute(lcClient).collectList().block().size());
		// the older one must be removed
		Assertions.assertEquals(1, SelectQuery.from(MySubEntity2.class, "entity").execute(lcClient).collectList().block().size());
		
		repo2.delete(entity).block();
		Assertions.assertEquals(0, SelectQuery.from(MyEntity2.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(0, SelectQuery.from(MySubEntity2.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testEntity3WithSubEntity() {
		MyEntity3 entity = new MyEntity3();
		entity.setValue("test");
		MySubEntity3 subEntity = new MySubEntity3();
		subEntity.setSubValue("sub test");
		entity.setSubEntity(subEntity);
		subEntity.setParent(entity);
		repo3.save(entity).block();
		
		List<MyEntity3> list = repo3.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test", entity.getValue());
		Assertions.assertNull(entity.getSubEntity());
		entity.lazyGetSubEntity().block();
		Assertions.assertNotNull(entity.getSubEntity());
		Assertions.assertEquals("sub test", entity.getSubEntity().getSubValue());
		
		repo3.delete(entity).block();
		list = SelectQuery.from(MyEntity3.class, "entity").execute(lcClient).collectList().block();
		Assertions.assertEquals(0, list.size());
		// sub entity should not be deleted
		List<MySubEntity3> subList = SelectQuery.from(MySubEntity3.class, "entity").execute(lcClient).collectList().block();
		Assertions.assertEquals(1, subList.size());
		// link must be set to null
		Assertions.assertNull(subList.get(0).getParent());
		
		// create a new entity, attached to the existing sub-entity
		entity = new MyEntity3();
		entity.setValue("second");
		entity.setSubEntity(subList.get(0));
		subList.get(0).setParent(entity);
		repo3.save(entity).block();
		
		list = repo3.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		Assertions.assertNull(list.get(0).getSubEntity());
		Assertions.assertNotNull(list.get(0).lazyGetSubEntity().block());
		Assertions.assertNotNull(list.get(0).getSubEntity());
		subList = SelectQuery.from(MySubEntity3.class, "entity").execute(lcClient).collectList().block();
		Assertions.assertEquals(1, subList.size());
		Assertions.assertEquals(subList.get(0).getId(), list.get(0).getSubEntity().getId());
	}
	
	@Test
	public void testInsertAndDelete100000EntitiesAnd50000LinkedEntities() {
		lcClient.save(Flux.range(0, 100000)
			.map(i -> {
				MyEntity1 entity = new MyEntity1();
				entity.setValue("entity" + i);
				if ((i % 2) == 0) {
					MySubEntity1 sub = new MySubEntity1();
					sub.setSubValue("sub" + i);
					sub.setParent(entity);
					entity.setSubEntity(sub);
				}
				return entity;
			})
		).then().block();
		
		repo1.deleteAll().block();
		
		Assertions.assertEquals(0, repo1.findAll().collectList().block().size());
	}
	
}
