package net.lecousin.reactive.data.relational.test.onetoonemodel;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
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
	
	@Autowired
	private MyEntity4Repository repo4;
	
	@Autowired
	private MySubEntity1Repository subRepo1;
	
	@Autowired
	private MySubEntity5Repository subRepo5;
	
	@Autowired
	private MySubEntity6Repository subRepo6;

	@Autowired
	private MyEntity1WithConstructorRepository repo1Ctor;
	
	@Override
	protected Collection<Class<?>> usedEntities() {
		return Arrays.asList(
			MyEntity1.class, MySubEntity1.class,
			MyEntity2.class, MySubEntity2.class,
			MyEntity3.class, MySubEntity3.class,
			MyEntity4.class, MySubEntity4.class,
			MyEntity5.class, MySubEntity5.class,
			MyEntity6.class, MySubEntity6.class,
			MyEntity1WithConstructor.class, MySubEntity1WithConstructor.class
		);
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
		Assertions.assertTrue(entity.getSubEntity().getParent() == entity);
		
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
	public void testConstructorEntity1WithoutSubEntity() {
		MyEntity1WithConstructor entity = new MyEntity1WithConstructor(null, "test", null);
		repo1Ctor.save(entity).block();
		
		List<MyEntity1WithConstructor> list = repo1Ctor.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test", entity.getValue());
		Assertions.assertNull(entity.getSubEntity());
		
		entity.setValue("modified");
		repo1Ctor.save(entity).block();
		list = repo1Ctor.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("modified", entity.getValue());
		Assertions.assertNull(entity.getSubEntity());
		Assertions.assertNull(entity.lazyGetSubEntity().block());
		Assertions.assertNull(entity.lazyGetSubEntity().block());
		
		repo1Ctor.deleteAll(list).block();
		Assertions.assertEquals(0, repo1Ctor.findAll().collectList().block().size());
	}
	
	@Test
	public void testConstructorEntity1WithSubEntity() {
		MyEntity1WithConstructor entity = new MyEntity1WithConstructor(null, "test", null);
		MySubEntity1WithConstructor subEntity = new MySubEntity1WithConstructor(null, "sub test", entity);
		entity.setSubEntity(subEntity);
		repo1Ctor.save(entity).block();
		
		List<MyEntity1WithConstructor> list = repo1Ctor.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test", entity.getValue());
		Assertions.assertNull(entity.getSubEntity());
		subEntity = entity.lazyGetSubEntity().block();
		Assertions.assertEquals("sub test", subEntity.getSubValue());
		Assertions.assertEquals("sub test", entity.getSubEntity().getSubValue());
		Assertions.assertTrue(subEntity == entity.lazyGetSubEntity().block());
		
		list = repo1Ctor.findByValue("abcd").collectList().block();
		Assertions.assertEquals(0, list.size());
		
		list = repo1Ctor.findByValue("test").collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test", entity.getValue());
		Assertions.assertNotNull(entity.getSubEntity());
		Assertions.assertEquals("sub test", entity.getSubEntity().getSubValue());
		Assertions.assertTrue(entity.getSubEntity().getParent() == entity);
		
		// update entity value
		entity.setValue("test 2");
		repo1Ctor.save(entity).block();
		list = repo1Ctor.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test 2", entity.getValue());
		subEntity = entity.lazyGetSubEntity().block();
		Assertions.assertEquals("sub test", subEntity.getSubValue());
		Assertions.assertEquals("sub test", entity.getSubEntity().getSubValue());
		
		// update sub entity value, save parent entity
		subEntity.setSubValue("sub test 2");
		repo1Ctor.save(entity).block();
		list = repo1Ctor.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test 2", entity.getValue());
		subEntity = entity.lazyGetSubEntity().block();
		Assertions.assertEquals("sub test 2", subEntity.getSubValue());
		Assertions.assertEquals("sub test 2", entity.getSubEntity().getSubValue());
		Assertions.assertEquals(1, SelectQuery.from(MyEntity1WithConstructor.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(1, SelectQuery.from(MySubEntity1WithConstructor.class, "entity").execute(lcClient).collectList().block().size());
		
		// change sub entity
		subEntity = new MySubEntity1WithConstructor(null, "new one", entity);
		entity.setSubEntity(subEntity);
		repo1Ctor.save(entity).block();
		list = repo1Ctor.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		entity = list.get(0);
		Assertions.assertEquals("test 2", entity.getValue());
		subEntity = entity.lazyGetSubEntity().block();
		Assertions.assertEquals("new one", subEntity.getSubValue());
		Assertions.assertEquals("new one", entity.getSubEntity().getSubValue());
		Assertions.assertEquals(1, SelectQuery.from(MyEntity1WithConstructor.class, "entity").execute(lcClient).collectList().block().size());
		// the old one must be removed
		Assertions.assertEquals(1, SelectQuery.from(MySubEntity1WithConstructor.class, "entity").execute(lcClient).collectList().block().size());
		
		repo1Ctor.delete(entity).block();
		Assertions.assertEquals(0, SelectQuery.from(MyEntity1WithConstructor.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(0, SelectQuery.from(MySubEntity1WithConstructor.class, "entity").execute(lcClient).collectList().block().size());
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
	public void testInsertAndDeleteManyEntities() {
		final int nb = lcClient.getSchemaDialect().isMultipleInsertSupported() ? 100000 : 1000;
		lcClient.save(Flux.range(0, nb)
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
		Assertions.assertEquals(nb, repo1.count().block());
		Assertions.assertEquals(nb / 2, SelectQuery.from(MySubEntity1.class, "e").executeCount(lcClient).block());
		
		repo1.deleteAll().block();
		
		Assertions.assertEquals(0, repo1.findAll().collectList().block().size());
	}
	
	@Test
	public void testDeleteById1() {
		List<MyEntity1> entities = lcClient.save(Flux.range(0, 10)
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
		).collectList().block();
		Assertions.assertEquals(10, entities.size());
		Assertions.assertEquals(10, repo1.count().block());
		
		int nb = 10;
		for (MyEntity1 entity : entities) {
			repo1.deleteById(entity.getId()).block();
			Assertions.assertEquals(--nb, repo1.count().block());
			Assertions.assertTrue(repo1.findById(entity.getId()).blockOptional().isEmpty());
		}
		Assertions.assertEquals(0, repo1.count().block());
		
		entities = lcClient.save(Flux.range(0, 10)
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
		).collectList().block();
		Assertions.assertEquals(10, entities.size());
		Assertions.assertEquals(10, repo1.count().block());
		
		repo1.deleteById(Flux.fromIterable(entities).map(MyEntity1::getId)).block();
		Assertions.assertEquals(0, repo1.count().block());
		
		entities = lcClient.save(Flux.range(0, 10)
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
		).collectList().block();
		Assertions.assertEquals(10, entities.size());
		Assertions.assertEquals(10, repo1.count().block());
		Assertions.assertEquals(5, subRepo1.count().block());
		
		MySubEntity1 sub = repo1.findByValue("entity4").blockFirst().lazyGetSubEntity().block();
		Assertions.assertEquals("sub4", sub.getSubValue());
		subRepo1.deleteById(sub.getId()).block();
		Assertions.assertEquals(10, repo1.count().block());
		Assertions.assertEquals(4, subRepo1.count().block());
		Assertions.assertNull(repo1.findByValue("entity4").blockFirst().lazyGetSubEntity().block());
	}
	
	@Test
	public void testDeleteById5HavingCascadeDelete() {
		List<MyEntity5> entities = lcClient.save(Flux.range(0, 10)
			.map(i -> {
				MyEntity5 entity = new MyEntity5();
				entity.setValue("entity" + i);
				MySubEntity5 sub = new MySubEntity5();
				sub.setSubValue("sub" + i);
				sub.setParent(entity);
				entity.setSubEntity(sub);
				return entity;
			})
		).collectList().block();
		Assertions.assertEquals(10, entities.size());
		Assertions.assertEquals(10, SelectQuery.from(MyEntity5.class, "e").executeCount(lcClient).block());
		Assertions.assertEquals(10, subRepo5.count().block());
		
		MySubEntity5 sub = entities.get(3).getSubEntity();
		Assertions.assertEquals("sub3", sub.getSubValue());
		subRepo5.deleteById(sub.getId()).block();
		Assertions.assertEquals(9, SelectQuery.from(MyEntity5.class, "e").executeCount(lcClient).block());
		Assertions.assertEquals(9, subRepo5.count().block());
	}
	
	@Test
	public void testDeleteById6HavingForeignTableWithOptionalFalse() {
		List<MyEntity6> entities = lcClient.save(Flux.range(0, 10)
			.map(i -> {
				MyEntity6 entity = new MyEntity6();
				entity.setValue("entity" + i);
				MySubEntity6 sub = new MySubEntity6();
				sub.setSubValue("sub" + i);
				sub.setParent(entity);
				entity.setSubEntity(sub);
				return entity;
			})
		).collectList().block();
		Assertions.assertEquals(10, entities.size());
		Assertions.assertEquals(10, SelectQuery.from(MyEntity6.class, "e").executeCount(lcClient).block());
		Assertions.assertEquals(10, subRepo6.count().block());
		
		MySubEntity6 sub = entities.get(3).getSubEntity();
		Assertions.assertEquals("sub3", sub.getSubValue());
		subRepo6.deleteById(sub.getId()).block();
		Assertions.assertEquals(9, SelectQuery.from(MyEntity6.class, "e").executeCount(lcClient).block());
		Assertions.assertEquals(9, subRepo6.count().block());
	}
	
	@Test
	public void testOneToOneWithCompositeIdInSubEntity() {
		List<MyEntity4> entities = repo4.saveAll(Flux.range(1, 10).map(i -> {
			MyEntity4 e = new MyEntity4();
			e.setValue("entity" + i);
			MySubEntity4 s = new MySubEntity4();
			s.setValue1(i + "." + 1);
			s.setValue2(i + "." + 2);
			s.setParent(e);
			e.setSubEntity(s);
			MyEntity1 e1 = new MyEntity1();
			e1.setValue("one." + i);
			s.setEntity1(e1);
			return e;
		})).collectList().block();
		Assertions.assertEquals(10, entities.size());
		Assertions.assertEquals(10, repo4.count().block());
		Assertions.assertEquals(10, SelectQuery.from(MySubEntity4.class, "e").executeCount(lcClient).block());
		Assertions.assertEquals(10, repo1.count().block());
		
		entities = repo4.findAll().collectList().block();
		Assertions.assertEquals(10, entities.size());
		for (MyEntity4 e : entities) {
			Assertions.assertTrue(e.getValue().startsWith("entity"));
			int i = Integer.parseInt(e.getValue().substring(6));
			Assertions.assertNull(e.getSubEntity());
			MySubEntity4 s = e.lazyGetSubEntity().block();
			Assertions.assertEquals(i + "." + 1, s.getValue1());
			Assertions.assertEquals(i + "." + 2, s.getValue2());
			Assertions.assertEquals(s, e.getSubEntity());
			Assertions.assertNotNull(s.getEntity1()); // id loaded
			Assertions.assertNull(s.getEntity1().getValue()); // but not value column
			MyEntity1 e1 = s.lazyGetEntity1().block();
			Assertions.assertEquals("one." + i, e1.getValue());
			Assertions.assertEquals("one." + i, s.getEntity1().getValue());
		}
		
		MySubEntity4 sub = new MySubEntity4();
		sub.setValue1("4.1");
		sub.setValue2("4.2");
		Assertions.assertNull(sub.getParent());
		lcClient.lazyLoad(sub).block();
		Assertions.assertNotNull(sub.getParent());
		Assertions.assertNull(sub.getParent().getValue());
		lcClient.lazyLoad(sub.getParent()).block();
		Assertions.assertEquals("entity4", sub.getParent().getValue());
		
		
		repo4.deleteAll().block();
		Assertions.assertEquals(0, repo4.count().block());
		Assertions.assertEquals(0, SelectQuery.from(MySubEntity4.class, "e").executeCount(lcClient).block());
		Assertions.assertEquals(0, repo1.count().block());
		
		entities = repo4.saveAll(Flux.range(1, 10).map(i -> {
			MyEntity4 e = new MyEntity4();
			e.setValue("entity" + i);
			MySubEntity4 s = new MySubEntity4();
			s.setValue1(i + "." + 1);
			s.setValue2(i + "." + 2);
			s.setParent(e);
			e.setSubEntity(s);
			MyEntity1 e1 = new MyEntity1();
			e1.setValue("one." + i);
			s.setEntity1(e1);
			return e;
		})).collectList().block();
		Assertions.assertEquals(10, entities.size());
		Assertions.assertEquals(10, repo4.count().block());
		Assertions.assertEquals(10, SelectQuery.from(MySubEntity4.class, "e").executeCount(lcClient).block());
		Assertions.assertEquals(10, repo1.count().block());
		
		entities = repo4.findAllWithSubEntities().collectList().block();
		Assertions.assertEquals(10, entities.size());
		for (MyEntity4 e : entities) {
			Assertions.assertTrue(e.getValue().startsWith("entity"));
			int i = Integer.parseInt(e.getValue().substring(6));
			MySubEntity4 s = e.getSubEntity();
			Assertions.assertNotNull(s);
			Assertions.assertEquals(i + "." + 1, s.getValue1());
			Assertions.assertEquals(i + "." + 2, s.getValue2());
			Assertions.assertEquals(s, e.getSubEntity());
			MyEntity1 e1 = s.getEntity1();
			Assertions.assertNotNull(e1);
			Assertions.assertNotNull(e1.getValue()); // but not value column
			Assertions.assertEquals("one." + i, e1.getValue());
			Assertions.assertEquals("one." + i, s.getEntity1().getValue());
		}
		repo4.deleteAll(entities).block();
		Assertions.assertEquals(0, repo4.count().block());
		Assertions.assertEquals(0, SelectQuery.from(MySubEntity4.class, "e").executeCount(lcClient).block());
		Assertions.assertEquals(0, repo1.count().block());
	}
	
	@Test
	public void testInvalidJoin() {
		SelectQuery<MyEntity4> q = SelectQuery.from(MyEntity4.class, "e").join("e", "subEntity", "s");
		Assertions.assertThrows(IllegalArgumentException.class, () -> q.join("subEntity", "entity1", "e1"));
	}
	
	@Test
	public void testCriteriaOnForeignKey() {
		List<MyEntity1> entities = lcClient.save(Flux.range(0, 10)
			.map(i -> {
				MyEntity1 entity = new MyEntity1();
				entity.setValue("entity" + i);
				MySubEntity1 sub = new MySubEntity1();
				sub.setSubValue("sub" + i);
				sub.setParent(entity);
				entity.setSubEntity(sub);
				return entity;
			})
		).collectList().block();
		Assertions.assertEquals(10, entities.size());
		Assertions.assertEquals(10, repo1.count().block());
		Assertions.assertEquals(10, subRepo1.count().block());
		
		MyEntity1 e1 = entities.get(6);
		List<MySubEntity1> list = SelectQuery.from(MySubEntity1.class, "sub").where(Criteria.property("sub", "parent").is(e1)).execute(lcClient).collectList().block();
		Assertions.assertEquals(1, list.size());
		Assertions.assertEquals(e1.getSubEntity().getId(), list.get(0).getId());
		
		list = SelectQuery.from(MySubEntity1.class, "sub").where(Criteria.property("sub", "parent").isNot(e1)).execute(lcClient).collectList().block();
		Assertions.assertEquals(9, list.size());
	}

}
