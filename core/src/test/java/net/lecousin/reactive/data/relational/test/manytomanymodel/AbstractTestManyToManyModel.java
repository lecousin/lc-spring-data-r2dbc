package net.lecousin.reactive.data.relational.test.manytomanymodel;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;
import net.lecousin.reactive.data.relational.test.AbstractLcReactiveDataRelationalTest;

@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
public abstract class AbstractTestManyToManyModel extends AbstractLcReactiveDataRelationalTest {

	@Autowired
	private Entity1Repository repo1;

	@Autowired
	private Entity3Repository repo3;
	
	@Override
	protected Collection<Class<?>> usedEntities() {
		return Arrays.asList(Entity1.class, Entity2.class, JoinEntity.class, Entity3.class, Entity4.class);
	}
	
	@Test
	public void testManualJoin() {
		/*
		 * e2_1 -> e1_1, e1_2
		 * e2_2 -> e1_3
		 * e2_3 -> e1_4, e1_5, e1_1
		 * ----
		 * e1_1 -> e2_1, e2_3
		 * e1_2 -> e2_1
		 * e1_3 -> e2_2
		 * e1_4 -> e2_3
		 * e1_5 -> e2_3
		 */
		Entity1 e1_1 = new Entity1();
		e1_1.setValue("1.1");
		e1_1.setLinks(new LinkedList<>());

		Entity1 e1_2 = new Entity1();
		e1_2.setValue("1.2");
		e1_2.setLinks(new LinkedList<>());

		Entity1 e1_3 = new Entity1();
		e1_3.setValue("1.3");
		e1_3.setLinks(new LinkedList<>());

		Entity1 e1_4 = new Entity1();
		e1_4.setValue("1.4");
		e1_4.setLinks(new LinkedList<>());

		Entity1 e1_5 = new Entity1();
		e1_5.setValue("1.5");
		e1_5.setLinks(new LinkedList<>());
		
		Entity2 e2_1 = new Entity2();
		e2_1.setValue("2.1");
		e2_1.setLinks(new LinkedList<>());
		
		Entity2 e2_2 = new Entity2();
		e2_2.setValue("2.2");
		e2_2.setLinks(new LinkedList<>());
		
		Entity2 e2_3 = new Entity2();
		e2_3.setValue("2.3");
		e2_3.setLinks(new LinkedList<>());
		
		JoinEntity j_1_1 = new JoinEntity();
		j_1_1.setEntity1(e1_1);
		j_1_1.setEntity2(e2_1);
		e1_1.getLinks().add(j_1_1);
		e2_1.getLinks().add(j_1_1);
		
		JoinEntity j_2_1 = new JoinEntity();
		j_2_1.setEntity1(e1_2);
		j_2_1.setEntity2(e2_1);
		e1_2.getLinks().add(j_2_1);
		e2_1.getLinks().add(j_2_1);
		
		JoinEntity j_3_2 = new JoinEntity();
		j_3_2.setEntity1(e1_3);
		j_3_2.setEntity2(e2_2);
		e1_3.getLinks().add(j_3_2);
		e2_2.getLinks().add(j_3_2);
		
		JoinEntity j_4_3 = new JoinEntity();
		j_4_3.setEntity1(e1_4);
		j_4_3.setEntity2(e2_3);
		e1_4.getLinks().add(j_4_3);
		e2_3.getLinks().add(j_4_3);
		
		JoinEntity j_5_3 = new JoinEntity();
		j_5_3.setEntity1(e1_5);
		j_5_3.setEntity2(e2_3);
		e1_5.getLinks().add(j_5_3);
		e2_3.getLinks().add(j_5_3);
		
		JoinEntity j_1_3 = new JoinEntity();
		j_1_3.setEntity1(e1_1);
		j_1_3.setEntity2(e2_3);
		e1_1.getLinks().add(j_1_3);
		e2_3.getLinks().add(j_1_3);
		
		List<Entity1> list1 = repo1.saveAll(Arrays.asList(e1_1, e1_2, e1_3, e1_4, e1_5)).collectList().block();
		Assertions.assertEquals(5, list1.size());
		
		list1 = repo1.findByEntity1Value("1.3").collectList().block();
		Assertions.assertEquals(1, list1.size());
		Assertions.assertEquals("1.3", list1.get(0).getValue());
		Assertions.assertEquals(1, list1.get(0).getLinks().size());
		Assertions.assertEquals("2.2", list1.get(0).getLinks().get(0).getEntity2().getValue());
		
		list1 = repo1.findByEntity1Value("1.5").collectList().block();
		Assertions.assertEquals(1, list1.size());
		Assertions.assertEquals("1.5", list1.get(0).getValue());
		Assertions.assertEquals(1, list1.get(0).getLinks().size());
		Assertions.assertEquals("2.3", list1.get(0).getLinks().get(0).getEntity2().getValue());
		Assertions.assertEquals(3, list1.get(0).getLinks().get(0).getEntity2().lazyGetLinks().collectList().block().size());

		
		list1 = repo1.findByEntity2Value("2.3").collectList().block();
		Assertions.assertEquals(3, list1.size());
		
		list1 = repo1.findByLinkedEntity1Value("1.1").collectList().block();
		Assertions.assertEquals(4, list1.size());
		
		Assertions.assertEquals(5, SelectQuery.from(Entity1.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(3, SelectQuery.from(Entity2.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(6, SelectQuery.from(JoinEntity.class, "entity").execute(lcClient).collectList().block().size());
		
		list1 = repo1.findWithLinks().collectList().block();
		Assertions.assertEquals(5, list1.size());
		
		// remove link e1_3 -> e2_2 ==> e1_3 and e2_2 become orphans
		e1_3 = list1.stream().filter(e -> "1.3".equals(e.getValue())).findFirst().get();
		JoinEntity link = e1_3.getLinks().stream().filter(l -> "2.2".equals(l.getEntity2().getValue())).findFirst().get();
		e1_3.getLinks().remove(link);
		repo1.save(e1_3).block();
		
		Assertions.assertEquals(5, SelectQuery.from(Entity1.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(3, SelectQuery.from(Entity2.class, "entity").execute(lcClient).collectList().block().size());
		// one link removed
		Assertions.assertEquals(5, SelectQuery.from(JoinEntity.class, "entity").execute(lcClient).collectList().block().size());
	}

	
	@Test
	public void testAutomaticJoin() {
		/*
		 * e2_1 -> e1_1, e1_2
		 * e2_2 -> e1_3
		 * e2_3 -> e1_4, e1_5, e1_1
		 * ----
		 * e1_1 -> e2_1, e2_3
		 * e1_2 -> e2_1
		 * e1_3 -> e2_2
		 * e1_4 -> e2_3
		 * e1_5 -> e2_3
		 */
		Entity3 e1_1 = new Entity3();
		e1_1.setValue("1.1");
		e1_1.setLinks(new HashSet<>());

		Entity3 e1_2 = new Entity3();
		e1_2.setValue("1.2");
		e1_2.setLinks(new HashSet<>());

		Entity3 e1_3 = new Entity3();
		e1_3.setValue("1.3");
		e1_3.setLinks(new HashSet<>());

		Entity3 e1_4 = new Entity3();
		e1_4.setValue("1.4");
		e1_4.setLinks(new HashSet<>());

		Entity3 e1_5 = new Entity3();
		e1_5.setValue("1.5");
		e1_5.setLinks(new HashSet<>());
		
		Entity4 e2_1 = new Entity4();
		e2_1.setValue("2.1");
		e2_1.setLinks(new HashSet<>());
		
		Entity4 e2_2 = new Entity4();
		e2_2.setValue("2.2");
		e2_2.setLinks(new HashSet<>());
		
		Entity4 e2_3 = new Entity4();
		e2_3.setValue("2.3");
		e2_3.setLinks(new HashSet<>());
		
		e1_1.getLinks().add(e2_1);
		e2_1.getLinks().add(e1_1);
		
		e1_2.getLinks().add(e2_1);
		e2_1.getLinks().add(e1_2);
		
		e1_3.getLinks().add(e2_2);
		e2_2.getLinks().add(e1_3);
		
		e1_4.getLinks().add(e2_3);
		e2_3.getLinks().add(e1_4);
		
		e1_5.getLinks().add(e2_3);
		e2_3.getLinks().add(e1_5);
		
		e1_1.getLinks().add(e2_3);
		e2_3.getLinks().add(e1_1);
		
		List<Entity3> list1 = repo3.saveAll(Arrays.asList(e1_1, e1_2, e1_3, e1_4, e1_5)).collectList().block();
		Assertions.assertEquals(5, list1.size());
		
		Assertions.assertEquals(5, SelectQuery.from(Entity3.class, "e").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(3, SelectQuery.from(Entity4.class, "e").execute(lcClient).collectList().block().size());
		
		list1 = repo3.findByEntity3Value("1.3").collectList().block();
		Assertions.assertEquals(1, list1.size());
		Assertions.assertEquals("1.3", list1.get(0).getValue());
		Assertions.assertEquals(1, list1.get(0).getLinks().size());
		Set<Entity4> links = list1.get(0).getLinks();
		Entity4 e4 = links.iterator().next();
		Assertions.assertEquals("2.2", e4.getValue());
		// test JoinTableCollectionToTargetCollection methods
		Assertions.assertFalse(links.isEmpty());
		Assertions.assertTrue(links.contains(e4));
		Assertions.assertFalse(links.contains(null));
		Assertions.assertFalse(links.contains(new Object()));
		Assertions.assertTrue(links.containsAll(Arrays.asList(e4)));
		Assertions.assertFalse(links.containsAll(Arrays.asList(e4, new Object())));
		Assertions.assertFalse(links.containsAll(Arrays.asList(new Object(), e4)));
		Assertions.assertFalse(links.containsAll(Arrays.asList(e4, null)));
		Assertions.assertEquals(1, links.toArray().length);
		Assertions.assertEquals(e4, links.toArray()[0]);
		Assertions.assertEquals(1, links.toArray(new Object[0]).length);
		Assertions.assertEquals(e4, links.toArray(new Object[10])[0]);
		Assertions.assertFalse(links.add(e4));
		Assertions.assertEquals(1, links.size());
		Assertions.assertFalse(links.remove(new Object()));
		Assertions.assertEquals(1, links.size());
		Assertions.assertFalse(links.retainAll(Arrays.asList(e4)));
		
		// remove and add link
		Assertions.assertTrue(list1.get(0).getLinks().remove(list1.get(0).getLinks().iterator().next()));
		Assertions.assertEquals(0, list1.get(0).getLinks().size());
		repo3.save(list1.get(0)).block();
		list1 = repo3.findByEntity3Value("1.3").collectList().block();
		Assertions.assertEquals(1, list1.size());
		Assertions.assertEquals(0, list1.get(0).getLinks().size());
		
		list1.get(0).getLinks().add(e2_2);
		repo3.save(list1.get(0)).block();
		list1 = repo3.findByEntity3Value("1.3").collectList().block();
		Assertions.assertEquals(1, list1.size());
		Assertions.assertEquals(1, list1.get(0).getLinks().size());

		list1.get(0).getLinks().removeAll(Arrays.asList(null, list1.get(0).getLinks().iterator().next(), new Object()));
		repo3.save(list1.get(0)).block();
		list1 = repo3.findByEntity3Value("1.3").collectList().block();
		Assertions.assertEquals(1, list1.size());
		Assertions.assertEquals(0, list1.get(0).getLinks().size());
		
		list1.get(0).getLinks().addAll(Arrays.asList(e2_2));
		repo3.save(list1.get(0)).block();
		list1 = repo3.findByEntity3Value("1.3").collectList().block();
		Assertions.assertEquals(1, list1.size());
		Assertions.assertEquals(1, list1.get(0).getLinks().size());

		list1.get(0).getLinks().retainAll(Arrays.asList(null, new Object()));
		repo3.save(list1.get(0)).block();
		list1 = repo3.findByEntity3Value("1.3").collectList().block();
		Assertions.assertEquals(1, list1.size());
		Assertions.assertEquals(0, list1.get(0).getLinks().size());
		
		list1.get(0).getLinks().add(e2_2);
		repo3.save(list1.get(0)).block();
		list1 = repo3.findByEntity3Value("1.3").collectList().block();
		Assertions.assertEquals(1, list1.size());
		Assertions.assertEquals(1, list1.get(0).getLinks().size());
		
		
		list1 = repo3.findByEntity3Value("1.5").collectList().block();
		Assertions.assertEquals(1, list1.size());
		Assertions.assertEquals("1.5", list1.get(0).getValue());
		Assertions.assertEquals(1, list1.get(0).getLinks().size());
		Assertions.assertEquals("2.3", list1.get(0).getLinks().iterator().next().getValue());
		Assertions.assertEquals(3, list1.get(0).getLinks().iterator().next().lazyGetLinks().collectList().block().size());

		
		list1 = repo3.findByEntity4Value("2.3").collectList().block();
		Assertions.assertEquals(3, list1.size());
		
		list1 = repo3.findByLinkedEntity3Value("1.1").collectList().block();
		Assertions.assertEquals(4, list1.size());
		
		Assertions.assertEquals(5, SelectQuery.from(Entity3.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(3, SelectQuery.from(Entity4.class, "entity").execute(lcClient).collectList().block().size());
		//Assertions.assertEquals(6, SelectQuery.from(JoinEntity.class, "entity").execute(lcClient).collectList().block().size());
		
		list1 = repo3.findWithLinks().collectList().block();
		Assertions.assertEquals(5, list1.size());
		Assertions.assertEquals(2, list1.stream().filter(e -> "1.1".equals(e.getValue())).findFirst().get().getLinks().size());
		Assertions.assertEquals(1, list1.stream().filter(e -> "1.2".equals(e.getValue())).findFirst().get().getLinks().size());
		Assertions.assertEquals(1, list1.stream().filter(e -> "1.3".equals(e.getValue())).findFirst().get().getLinks().size());
		Assertions.assertEquals(1, list1.stream().filter(e -> "1.4".equals(e.getValue())).findFirst().get().getLinks().size());
		Assertions.assertEquals(1, list1.stream().filter(e -> "1.5".equals(e.getValue())).findFirst().get().getLinks().size());
		List<Entity4> list2 = SelectQuery.from(Entity4.class, "entity").execute(lcClient).collectList().block();
		Assertions.assertEquals(3, list2.size());
		Assertions.assertEquals(2, list2.stream().filter(e -> "2.1".equals(e.getValue())).findFirst().get().lazyGetLinks().collectList().block().size());
		Assertions.assertEquals(1, list2.stream().filter(e -> "2.2".equals(e.getValue())).findFirst().get().lazyGetLinks().collectList().block().size());
		Assertions.assertEquals(3, list2.stream().filter(e -> "2.3".equals(e.getValue())).findFirst().get().lazyGetLinks().collectList().block().size());
		
		
		// remove link e1_3 -> e2_2 ==> e1_3 and e2_2 become orphans
		e1_3 = list1.stream().filter(e -> "1.3".equals(e.getValue())).findFirst().get();
		Entity4 link = e1_3.getLinks().stream().filter(l -> "2.2".equals(l.getValue())).findFirst().get();
		e1_3.getLinks().remove(link);
		repo3.save(e1_3).block();
		
		Assertions.assertEquals(5, SelectQuery.from(Entity3.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(3, SelectQuery.from(Entity4.class, "entity").execute(lcClient).collectList().block().size());
		// e1_3 and e2_2 does not have any link
		list1 = repo3.findWithLinks().collectList().block();
		Assertions.assertEquals(5, list1.size());
		e1_3 = list1.stream().filter(e -> "1.3".equals(e.getValue())).findFirst().get();
		Assertions.assertEquals(0, e1_3.lazyGetLinks().collectList().block().size());
		e2_2 = SelectQuery.from(Entity4.class, "entity").execute(lcClient).collectList().block().stream().filter(e -> "2.2".equals(e.getValue())).findFirst().get();
		Assertions.assertEquals(0, e2_2.lazyGetLinks().collectList().block().size());
		// other links remain
		Assertions.assertEquals(2, list1.stream().filter(e -> "1.1".equals(e.getValue())).findFirst().get().getLinks().size());
		Assertions.assertEquals(1, list1.stream().filter(e -> "1.2".equals(e.getValue())).findFirst().get().getLinks().size());
		Assertions.assertEquals(0, list1.stream().filter(e -> "1.3".equals(e.getValue())).findFirst().get().getLinks().size());
		Assertions.assertEquals(1, list1.stream().filter(e -> "1.4".equals(e.getValue())).findFirst().get().getLinks().size());
		Assertions.assertEquals(1, list1.stream().filter(e -> "1.5".equals(e.getValue())).findFirst().get().getLinks().size());
		list2 = SelectQuery.from(Entity4.class, "entity").execute(lcClient).collectList().block();
		Assertions.assertEquals(3, list2.size());
		Assertions.assertEquals(2, list2.stream().filter(e -> "2.1".equals(e.getValue())).findFirst().get().lazyGetLinks().collectList().block().size());
		Assertions.assertEquals(0, list2.stream().filter(e -> "2.2".equals(e.getValue())).findFirst().get().lazyGetLinks().collectList().block().size());
		Assertions.assertEquals(3, list2.stream().filter(e -> "2.3".equals(e.getValue())).findFirst().get().lazyGetLinks().collectList().block().size());
	}
	
}
