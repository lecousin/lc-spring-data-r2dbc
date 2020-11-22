package net.lecousin.reactive.data.relational.test.manytomanymodel;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;
import net.lecousin.reactive.data.relational.test.AbstractLcReactiveDataRelationalTest;

@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
public class AbstractTestManyToManyModel extends AbstractLcReactiveDataRelationalTest {

	@Autowired
	private Entity1Repository repo1;
	
	@Test
	public void test() {
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
		
		Assertions.assertEquals(5, repo1.getLcClient().getSpringClient().select().from(Entity1.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(3, repo1.getLcClient().getSpringClient().select().from(Entity2.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(6, repo1.getLcClient().getSpringClient().select().from(JoinEntity.class).fetch().all().collectList().block().size());
		
		list1 = repo1.findWithLinks().collectList().block();
		Assertions.assertEquals(5, list1.size());
		
		// remove link e1_3 -> e2_2 ==> e1_3 and e2_2 become orphans
		e1_3 = list1.stream().filter(e -> "1.3".equals(e.getValue())).findFirst().get();
		JoinEntity link = e1_3.getLinks().stream().filter(l -> "2.2".equals(l.getEntity2().getValue())).findFirst().get();
		e1_3.getLinks().remove(link);
		repo1.save(e1_3).block();
		
		Assertions.assertEquals(5, repo1.getLcClient().getSpringClient().select().from(Entity1.class).fetch().all().collectList().block().size());
		Assertions.assertEquals(3, repo1.getLcClient().getSpringClient().select().from(Entity2.class).fetch().all().collectList().block().size());
		// one link removed
		Assertions.assertEquals(5, repo1.getLcClient().getSpringClient().select().from(JoinEntity.class).fetch().all().collectList().block().size());
	}

}
