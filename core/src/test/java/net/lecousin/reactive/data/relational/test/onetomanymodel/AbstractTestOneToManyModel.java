package net.lecousin.reactive.data.relational.test.onetomanymodel;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;
import net.lecousin.reactive.data.relational.test.AbstractLcReactiveDataRelationalTest;

@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
public class AbstractTestOneToManyModel extends AbstractLcReactiveDataRelationalTest {

	@Autowired
	private RootEntityRepository repo;
	
	@Test
	public void testListEmpty() {
		RootEntity root = new RootEntity();
		root.setValue("empty");
		root.setList(new LinkedList<>());
		root = repo.save(root).block();
		Assertions.assertEquals("empty", root.getValue());
		Assertions.assertTrue(root.getList() == null || root.getList().isEmpty());
		
		List<RootEntity> list = repo.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		root = list.get(0);
		Assertions.assertEquals("empty", root.getValue());
		Assertions.assertTrue(root.getList() == null || root.getList().isEmpty());
		
		list = repo.findByValueWithSubEntity("abcd").collectList().block();
		Assertions.assertEquals(0, list.size());
		
		list = repo.findByValueWithSubEntity("empty").collectList().block();
		Assertions.assertEquals(1, list.size());
		root = list.get(0);
		Assertions.assertEquals("empty", root.getValue());
		Assertions.assertTrue(root.getList() == null || root.getList().isEmpty());
		
		repo.deleteAll(repo.findAll().collectList().block()).block();
		Assertions.assertEquals(0, repo.findAll().collectList().block().size());
	}
	
	@Test
	public void testOneSubElement() {
		RootEntity root = new RootEntity();
		root.setValue("one");
		root.setList(new LinkedList<>());
		SubEntity sub = new SubEntity();
		sub.setSubValue("sub1");
		root.getList().add(sub);
		root = repo.save(root).block();
		Assertions.assertEquals("one", root.getValue());
		Assertions.assertNotNull(root.getList());
		Assertions.assertEquals(1, root.getList().size());
		Assertions.assertEquals("sub1", root.getList().get(0).getSubValue());
		Assertions.assertEquals(root, root.getList().get(0).getParent());
		
		List<RootEntity> list = repo.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		root = list.get(0);
		Assertions.assertEquals("one", root.getValue());
		Assertions.assertNull(root.getList());
		List<SubEntity> children = root.lazyGetList().collectList().block();
		Assertions.assertNotNull(children);
		Assertions.assertEquals(1, children.size());
		Assertions.assertEquals("sub1", children.get(0).getSubValue());
		children = root.getList();
		Assertions.assertNotNull(children);
		Assertions.assertEquals(1, children.size());
		Assertions.assertEquals("sub1", children.get(0).getSubValue());
		// test call lazyGet on an already loaded list
		root.lazyGetList().collectList().block();
		
		list = repo.findByValueWithSubEntity("abcd").collectList().block();
		Assertions.assertEquals(0, list.size());
		
		list = repo.findByValueWithSubEntity("one").collectList().block();
		Assertions.assertEquals(1, list.size());
		root = list.get(0);
		Assertions.assertEquals("one", root.getValue());
		Assertions.assertNotNull(root.getList());
		Assertions.assertEquals(1, root.getList().size());
		Assertions.assertEquals("sub1", root.getList().get(0).getSubValue());
		Assertions.assertEquals(root, root.getList().get(0).getParent());
		
		list = repo.findBySubValue("abcd").collectList().block();
		Assertions.assertEquals(0, list.size());
		
		list = repo.findBySubValue("sub1").collectList().block();
		Assertions.assertEquals(1, list.size());
		root = list.get(0);
		Assertions.assertEquals("one", root.getValue());
		Assertions.assertNotNull(root.getList());
		Assertions.assertEquals(1, root.getList().size());
		Assertions.assertEquals("sub1", root.getList().get(0).getSubValue());
		Assertions.assertEquals(root, root.getList().get(0).getParent());
		
		repo.deleteAll(repo.findAll().collectList().block()).block();
		Assertions.assertEquals(0, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(0, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testZeroToSeveralSubElements() {
		RootEntity root;
		SubEntity sub;

		root = new RootEntity();
		root.setValue("zero");
		repo.save(root).block();

		root = new RootEntity();
		root.setValue("one");
		root.setList(new LinkedList<>());
		sub = new SubEntity();
		sub.setSubValue("sub1.1");
		root.getList().add(sub);
		repo.save(root).block();

		root = new RootEntity();
		root.setValue("two");
		root.setList(new LinkedList<>());
		sub = new SubEntity();
		sub.setSubValue("sub2.1");
		root.getList().add(sub);
		sub = new SubEntity();
		sub.setSubValue("sub2.2");
		root.getList().add(sub);
		repo.save(root).block();

		root = new RootEntity();
		root.setValue("five");
		root.setList(new LinkedList<>());
		sub = new SubEntity();
		sub.setSubValue("sub5.1");
		root.getList().add(sub);
		sub = new SubEntity();
		sub.setSubValue("sub5.2");
		root.getList().add(sub);
		sub = new SubEntity();
		sub.setSubValue("sub5.3");
		root.getList().add(sub);
		sub = new SubEntity();
		sub.setSubValue("sub5.4");
		root.getList().add(sub);
		sub = new SubEntity();
		sub.setSubValue("sub5.5");
		root.getList().add(sub);
		repo.save(root).block();
		
		
		
		List<RootEntity> list = repo.findAll().collectList().block();
		Assertions.assertEquals(4, list.size());
		List<String> expected = new LinkedList<>();
		expected.addAll(Arrays.asList("zero", "one", "two", "five"));
		for (RootEntity r : list) {
			Assertions.assertTrue(expected.remove(r.getValue()));
			Assertions.assertNull(r.getList());
			List<SubEntity> children = r.lazyGetList().collectList().block();
			Assertions.assertNotNull(children);
			if ("zero".equals(r.getValue())) {
				Assertions.assertEquals(0, children.size());
			} else if ("one".equals(r.getValue())) {
				Assertions.assertEquals(1, children.size());
				Assertions.assertEquals("sub1.1", children.get(0).getSubValue());
			} else if ("two".equals(r.getValue())) {
				Assertions.assertEquals(2, children.size());
				List<String> expectedSub = new LinkedList<>();
				expectedSub.addAll(Arrays.asList("sub2.1", "sub2.2"));
				for (SubEntity s : children) {
					Assertions.assertTrue(expectedSub.remove(s.getSubValue()));
				}
			} else if ("five".equals(r.getValue())) {
				Assertions.assertEquals(5, children.size());
				List<String> expectedSub = new LinkedList<>();
				expectedSub.addAll(Arrays.asList("sub5.1", "sub5.2", "sub5.3", "sub5.4", "sub5.5"));
				for (SubEntity s : children) {
					Assertions.assertTrue(expectedSub.remove(s.getSubValue()));
				}
			}
			children = root.getList();
			Assertions.assertNotNull(children);
		}
		
		list = repo.findByValueWithSubEntity("abcd").collectList().block();
		Assertions.assertEquals(0, list.size());
		
		list = repo.findByValueWithSubEntity("zero").collectList().block();
		Assertions.assertEquals(1, list.size());
		root = list.get(0);
		Assertions.assertEquals("zero", root.getValue());
		Assertions.assertNotNull(root.getList());
		Assertions.assertEquals(0, root.getList().size());
		
		list = repo.findByValueWithSubEntity("one").collectList().block();
		Assertions.assertEquals(1, list.size());
		root = list.get(0);
		Assertions.assertEquals("one", root.getValue());
		Assertions.assertNotNull(root.getList());
		Assertions.assertEquals(1, root.getList().size());
		Assertions.assertEquals("sub1.1", root.getList().get(0).getSubValue());
		Assertions.assertEquals(root, root.getList().get(0).getParent());
		
		list = repo.findByValueWithSubEntity("two").collectList().block();
		Assertions.assertEquals(1, list.size());
		root = list.get(0);
		Assertions.assertEquals("two", root.getValue());
		Assertions.assertNotNull(root.getList());
		Assertions.assertEquals(2, root.getList().size());
		List<String> expectedSub = new LinkedList<>();
		expectedSub.addAll(Arrays.asList("sub2.1", "sub2.2"));
		for (SubEntity s : root.getList()) {
			Assertions.assertTrue(expectedSub.remove(s.getSubValue()));
			Assertions.assertEquals(root, s.getParent());
		}
		
		list = repo.findByValueWithSubEntity("five").collectList().block();
		Assertions.assertEquals(1, list.size());
		root = list.get(0);
		Assertions.assertEquals("five", root.getValue());
		Assertions.assertNotNull(root.getList());
		Assertions.assertEquals(5, root.getList().size());
		expectedSub = new LinkedList<>();
		expectedSub.addAll(Arrays.asList("sub5.1", "sub5.2", "sub5.3", "sub5.4", "sub5.5"));
		for (SubEntity s : root.getList()) {
			Assertions.assertTrue(expectedSub.remove(s.getSubValue()));
			Assertions.assertEquals(root, s.getParent());
		}
		
		list = repo.findBySubValue("abcd").collectList().block();
		Assertions.assertEquals(0, list.size());
		
		list = repo.findBySubValue("sub1.1").collectList().block();
		Assertions.assertEquals(1, list.size());
		root = list.get(0);
		Assertions.assertEquals("one", root.getValue());
		Assertions.assertNotNull(root.getList());
		Assertions.assertEquals(1, root.getList().size());
		Assertions.assertEquals("sub1.1", root.getList().get(0).getSubValue());
		Assertions.assertEquals(root, root.getList().get(0).getParent());
		
		list = repo.findBySubValue("sub2.1").collectList().block();
		Assertions.assertEquals(1, list.size());
		root = list.get(0);
		Assertions.assertEquals("two", root.getValue());
		Assertions.assertNotNull(root.getList());
		Assertions.assertEquals(2, root.getList().size());
		expectedSub = new LinkedList<>();
		expectedSub.addAll(Arrays.asList("sub2.1", "sub2.2"));
		for (SubEntity s : root.getList()) {
			Assertions.assertTrue(expectedSub.remove(s.getSubValue()));
			Assertions.assertEquals(root, s.getParent());
		}
		
		list = repo.findBySubValue("sub5.3").collectList().block();
		Assertions.assertEquals(1, list.size());
		root = list.get(0);
		Assertions.assertEquals("five", root.getValue());
		Assertions.assertNotNull(root.getList());
		Assertions.assertEquals(5, root.getList().size());
		expectedSub = new LinkedList<>();
		expectedSub.addAll(Arrays.asList("sub5.1", "sub5.2", "sub5.3", "sub5.4", "sub5.5"));
		for (SubEntity s : root.getList()) {
			Assertions.assertTrue(expectedSub.remove(s.getSubValue()));
			Assertions.assertEquals(root, s.getParent());
		}
		
		list = repo.findBySubValueStartsWith("sub5", 0, 10).collectList().block();
		Assertions.assertEquals(1, list.size());
		root = list.get(0);
		Assertions.assertEquals("five", root.getValue());
		Assertions.assertNotNull(root.getList());
		Assertions.assertEquals(5, root.getList().size());
		expectedSub = new LinkedList<>();
		expectedSub.addAll(Arrays.asList("sub5.1", "sub5.2", "sub5.3", "sub5.4", "sub5.5"));
		for (SubEntity s : root.getList()) {
			Assertions.assertTrue(expectedSub.remove(s.getSubValue()));
			Assertions.assertEquals(root, s.getParent());
		}

		list = repo.findBySubValueStartsWith("sub", 0, 10).collectList().block();
		Assertions.assertEquals(3, list.size());
		list = repo.findBySubValueStartsWith("sub", 0, 1).collectList().block();
		Assertions.assertEquals(1, list.size());
		list = repo.findBySubValueStartsWith("sub", 0, 2).collectList().block();
		Assertions.assertEquals(2, list.size());
		list = repo.findBySubValueStartsWith("sub", 0, 3).collectList().block();
		Assertions.assertEquals(3, list.size());
		list = repo.findBySubValueStartsWith("sub", 1, 1).collectList().block();
		Assertions.assertEquals(1, list.size());
		list = repo.findBySubValueStartsWith("sub", 1, 2).collectList().block();
		Assertions.assertEquals(2, list.size());
		list = repo.findBySubValueStartsWith("sub", 1, 3).collectList().block();
		Assertions.assertEquals(2, list.size());
		list = repo.findBySubValueStartsWith("sub", 2, 1).collectList().block();
		Assertions.assertEquals(1, list.size());
		list = repo.findBySubValueStartsWith("sub", 2, 2).collectList().block();
		Assertions.assertEquals(1, list.size());
		list = repo.findBySubValueStartsWith("sub", 3, 1).collectList().block();
		Assertions.assertEquals(0, list.size());
		
		
		Assertions.assertEquals(4, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(8, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
		
		repo.deleteAll(repo.findByValueWithSubEntity("two").collectList().block()).block();
		Assertions.assertEquals(3, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(6, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
		
		repo.deleteAll(repo.findByValueWithSubEntity("zero").collectList().block()).block();
		Assertions.assertEquals(2, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(6, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
		
		repo.deleteAll(repo.findByValueWithSubEntity("five").collectList().block()).block();
		Assertions.assertEquals(1, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(1, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
		
		repo.deleteAll(repo.findByValueWithSubEntity("one").collectList().block()).block();
		Assertions.assertEquals(0, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(0, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testHavingSubValueEqualsToValue() {
		RootEntity root;
		SubEntity sub;

		root = new RootEntity();
		root.setValue("zero");
		repo.save(root).block();

		root = new RootEntity();
		root.setValue("one");
		root.setList(new LinkedList<>());
		sub = new SubEntity();
		sub.setSubValue("one");
		root.getList().add(sub);
		repo.save(root).block();

		root = new RootEntity();
		root.setValue("two");
		root.setList(new LinkedList<>());
		sub = new SubEntity();
		sub.setSubValue("one");
		root.getList().add(sub);
		repo.save(root).block();

		root = new RootEntity();
		root.setValue("three");
		root.setList(new LinkedList<>());
		sub = new SubEntity();
		sub.setSubValue("one");
		root.getList().add(sub);
		sub = new SubEntity();
		sub.setSubValue("two");
		root.getList().add(sub);
		repo.save(root).block();

		root = new RootEntity();
		root.setValue("four");
		root.setList(new LinkedList<>());
		sub = new SubEntity();
		sub.setSubValue("one");
		root.getList().add(sub);
		sub = new SubEntity();
		sub.setSubValue("two");
		root.getList().add(sub);
		sub = new SubEntity();
		sub.setSubValue("three");
		root.getList().add(sub);
		sub = new SubEntity();
		sub.setSubValue("four");
		root.getList().add(sub);
		sub = new SubEntity();
		sub.setSubValue("five");
		root.getList().add(sub);
		repo.save(root).block();
		
		List<RootEntity> list = repo.havingSubValueEqualsToValue().collectList().block();
		Assertions.assertEquals(2, list.size());
		RootEntity r1 = list.get(0);
		RootEntity r2 = list.get(1);
		if ("one".equals(r1.getValue())) {
			Assertions.assertEquals(1, r1.getList().size());
			Assertions.assertEquals("four", r2.getValue());
			Assertions.assertEquals(5, r2.getList().size());
		} else if ("four".equals(r1.getValue())) {
			Assertions.assertEquals(5, r1.getList().size());
			Assertions.assertEquals("one", r2.getValue());
			Assertions.assertEquals(1, r2.getList().size());
		} else
			throw new AssertionError("Unexpected value " + r1.getValue());
		
		Assertions.assertEquals(5, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(9, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
		
		repo.deleteAll(repo.findBySubValue("two")).block();
		Assertions.assertEquals(3, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(2, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
		
		repo.deleteAll().block();
		Assertions.assertEquals(0, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(0, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testMultipleSubElements() {
		RootEntity root1 = new RootEntity();
		root1.setValue("one");
		RootEntity root2 = new RootEntity();
		root2.setValue("two");

		SubEntity sub1_1 = new SubEntity();
		sub1_1.setSubValue("1.1");
		SubEntity sub1_2 = new SubEntity();
		sub1_2.setSubValue("1.2");
		SubEntity sub1_3 = new SubEntity();
		sub1_3.setSubValue("1.3");
		
		SubEntity2 sub2_1 = new SubEntity2("2.1");
		SubEntity2 sub2_2 = new SubEntity2("2.2");
		SubEntity2 sub2_3 = new SubEntity2("2.3");
		
		SubEntity3 sub3_1 = new SubEntity3();
		sub3_1.setSubValue("3.1");
		SubEntity3 sub3_2 = new SubEntity3();
		sub3_2.setSubValue("3.2");
		SubEntity3 sub3_3 = new SubEntity3();
		sub3_3.setSubValue("3.3");
		
		root1.setList(Arrays.asList(sub1_1, sub1_2));
		root2.setList(Arrays.asList(sub1_3));
		root1.setList2(Arrays.asList(sub2_1, sub2_2));
		root2.setList2(Arrays.asList(sub2_3));
		root1.setList3(Arrays.asList(sub3_1, sub3_2));
		root2.setList3(Arrays.asList(sub3_3));
		
		repo.saveAll(Arrays.asList(root1, root2)).collectList().block();
		
		List<RootEntity> list = repo.findAll().collectList().block();
		Assertions.assertEquals(2, list.size());
		Assertions.assertNull(list.get(0).getList());
		Assertions.assertNull(list.get(0).getList2());
		Assertions.assertNull(list.get(0).getList3());
		Assertions.assertNull(list.get(1).getList());
		Assertions.assertNull(list.get(1).getList2());
		Assertions.assertNull(list.get(1).getList3());
		if ("one".equals(list.get(0).getValue())) {
			root1 = list.get(0);
			root2 = list.get(1);
		} else {
			root1 = list.get(1);
			root2 = list.get(0);
		}
		Assertions.assertEquals(2, root1.lazyGetList().collectList().block().size());
		Assertions.assertEquals(2, root1.lazyGetList2().collectList().block().size());
		Assertions.assertEquals(2, root1.lazyGetList3().collectList().block().size());
		Assertions.assertEquals(1, root2.lazyGetList().collectList().block().size());
		Assertions.assertEquals(1, root2.lazyGetList2().collectList().block().size());
		Assertions.assertEquals(1, root2.lazyGetList3().collectList().block().size());
		for (SubEntity s : root1.getList())
			Assertions.assertEquals(root1, s.getParent());
		for (SubEntity s : root2.getList())
			Assertions.assertEquals(root2, s.getParent());
		for (SubEntity2 s : root1.getList2())
			Assertions.assertEquals(root1, s.getParent());
		for (SubEntity2 s : root2.getList2())
			Assertions.assertEquals(root2, s.getParent());
		for (SubEntity3 s : root1.getList3())
			Assertions.assertEquals(root1, s.getParent());
		for (SubEntity3 s : root2.getList3())
			Assertions.assertEquals(root2, s.getParent());
		Assertions.assertEquals(1, root1.getList3().stream().filter(s -> "3.1".equals(s.getSubValue())).findFirst().get().getVersion());
		Assertions.assertEquals(1, root1.getList3().stream().filter(s -> "3.2".equals(s.getSubValue())).findFirst().get().getVersion());
		Assertions.assertEquals(1, root2.getList3().stream().filter(s -> "3.3".equals(s.getSubValue())).findFirst().get().getVersion());
		Assertions.assertEquals(2, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(3, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(3, SelectQuery.from(SubEntity2.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(3, SelectQuery.from(SubEntity3.class, "entity").execute(lcClient).collectList().block().size());
		
		list = repo.findAllFull().collectList().block();
		Assertions.assertEquals(2, list.size());
		Assertions.assertNotNull(list.get(0).getList());
		Assertions.assertNotNull(list.get(0).getList2());
		Assertions.assertNotNull(list.get(0).getList3());
		Assertions.assertNotNull(list.get(1).getList());
		Assertions.assertNotNull(list.get(1).getList2());
		Assertions.assertNotNull(list.get(1).getList3());
		if ("one".equals(list.get(0).getValue())) {
			root1 = list.get(0);
			root2 = list.get(1);
		} else {
			root1 = list.get(1);
			root2 = list.get(0);
		}
		Assertions.assertEquals(2, root1.getList().size());
		Assertions.assertEquals(2, root1.getList2().size());
		Assertions.assertEquals(2, root1.getList3().size());
		Assertions.assertEquals(1, root2.getList().size());
		Assertions.assertEquals(1, root2.getList2().size());
		Assertions.assertEquals(1, root2.getList3().size());
		for (SubEntity s : root1.getList())
			Assertions.assertEquals(root1, s.getParent());
		for (SubEntity s : root2.getList())
			Assertions.assertEquals(root2, s.getParent());
		for (SubEntity2 s : root1.getList2())
			Assertions.assertEquals(root1, s.getParent());
		for (SubEntity2 s : root2.getList2())
			Assertions.assertEquals(root2, s.getParent());
		for (SubEntity3 s : root1.getList3())
			Assertions.assertEquals(root1, s.getParent());
		for (SubEntity3 s : root2.getList3())
			Assertions.assertEquals(root2, s.getParent());
		Assertions.assertEquals(1, root1.getList3().stream().filter(s -> "3.1".equals(s.getSubValue())).findFirst().get().getVersion());
		Assertions.assertEquals(1, root1.getList3().stream().filter(s -> "3.2".equals(s.getSubValue())).findFirst().get().getVersion());
		Assertions.assertEquals(1, root2.getList3().stream().filter(s -> "3.3".equals(s.getSubValue())).findFirst().get().getVersion());
		
		
		// change 3.2
		root1.getList3().stream().filter(s -> "3.2".equals(s.getSubValue())).findFirst().get().setSubValue("3.2bis");
		repo.saveAll(Arrays.asList(root1, root2)).collectList().block();
		list = repo.findAllFull().collectList().block();
		Assertions.assertEquals(2, list.size());
		Assertions.assertNotNull(list.get(0).getList());
		Assertions.assertNotNull(list.get(0).getList2());
		Assertions.assertNotNull(list.get(0).getList3());
		Assertions.assertNotNull(list.get(1).getList());
		Assertions.assertNotNull(list.get(1).getList2());
		Assertions.assertNotNull(list.get(1).getList3());
		if ("one".equals(list.get(0).getValue())) {
			root1 = list.get(0);
			root2 = list.get(1);
		} else {
			root1 = list.get(1);
			root2 = list.get(0);
		}
		Assertions.assertEquals(2, root1.getList().size());
		Assertions.assertEquals(2, root1.getList2().size());
		Assertions.assertEquals(2, root1.getList3().size());
		Assertions.assertEquals(1, root2.getList().size());
		Assertions.assertEquals(1, root2.getList2().size());
		Assertions.assertEquals(1, root2.getList3().size());
		Assertions.assertEquals(1, root1.getList3().stream().filter(s -> "3.1".equals(s.getSubValue())).findFirst().get().getVersion());
		Assertions.assertEquals(2, root1.getList3().stream().filter(s -> "3.2bis".equals(s.getSubValue())).findFirst().get().getVersion());
		Assertions.assertEquals(1, root2.getList3().stream().filter(s -> "3.3".equals(s.getSubValue())).findFirst().get().getVersion());
		
		root1.getList3().stream().filter(s -> "3.2bis".equals(s.getSubValue())).findFirst().get().setSubValue("3.2");
		repo.saveAll(Arrays.asList(root1, root2)).collectList().block();
		list = repo.findAllFull().collectList().block();
		Assertions.assertEquals(2, list.size());
		Assertions.assertNotNull(list.get(0).getList());
		Assertions.assertNotNull(list.get(0).getList2());
		Assertions.assertNotNull(list.get(0).getList3());
		Assertions.assertNotNull(list.get(1).getList());
		Assertions.assertNotNull(list.get(1).getList2());
		Assertions.assertNotNull(list.get(1).getList3());
		if ("one".equals(list.get(0).getValue())) {
			root1 = list.get(0);
			root2 = list.get(1);
		} else {
			root1 = list.get(1);
			root2 = list.get(0);
		}
		Assertions.assertEquals(2, root1.getList().size());
		Assertions.assertEquals(2, root1.getList2().size());
		Assertions.assertEquals(2, root1.getList3().size());
		Assertions.assertEquals(1, root2.getList().size());
		Assertions.assertEquals(1, root2.getList2().size());
		Assertions.assertEquals(1, root2.getList3().size());
		Assertions.assertEquals(1, root1.getList3().stream().filter(s -> "3.1".equals(s.getSubValue())).findFirst().get().getVersion());
		Assertions.assertEquals(3, root1.getList3().stream().filter(s -> "3.2".equals(s.getSubValue())).findFirst().get().getVersion());
		Assertions.assertEquals(1, root2.getList3().stream().filter(s -> "3.3".equals(s.getSubValue())).findFirst().get().getVersion());
		

		// move in 2 steps
		
		// move sub1_1 from root1 to root2
		sub1_1 = root1.getList().stream().filter(s -> "1.1".equals(s.getSubValue())).findFirst().get();
		Assertions.assertTrue(root1.getList().remove(sub1_1));
		// move sub2_2 from root1 to root2
		sub2_2 = root1.getList2().stream().filter(s -> "2.2".equals(s.getSubValue())).findFirst().get();
		Assertions.assertTrue(root1.getList2().remove(sub2_2));
		// move sub3_3 from root2 to root1
		sub3_3 = root2.getList3().stream().filter(s -> "3.3".equals(s.getSubValue())).findFirst().get();
		Assertions.assertTrue(root2.getList3().remove(sub3_3)); // 3.3 version 2
		
		repo.saveAll(Arrays.asList(root1, root2)).collectList().block();

		list = repo.findAllFull().collectList().block();
		Assertions.assertEquals(2, list.size());
		if ("one".equals(list.get(0).getValue())) {
			root1 = list.get(0);
			root2 = list.get(1);
		} else {
			root1 = list.get(1);
			root2 = list.get(0);
		}
		Assertions.assertEquals(1, root1.getList().size());
		Assertions.assertEquals(1, root1.getList2().size());
		Assertions.assertEquals(2, root1.getList3().size());
		Assertions.assertEquals(1, root2.getList().size());
		Assertions.assertEquals(1, root2.getList2().size());
		Assertions.assertEquals(0, root2.getList3().size());
		
		root2.getList().add(sub1_1);
		root2.getList2().add(sub2_2);
		root1.getList3().add(sub3_3); // 3.3 version 3
		
		repo.saveAll(Arrays.asList(root1, root2)).collectList().block();
		
		list = repo.findAllFull().collectList().block();
		Assertions.assertEquals(2, list.size());
		if ("one".equals(list.get(0).getValue())) {
			root1 = list.get(0);
			root2 = list.get(1);
		} else {
			root1 = list.get(1);
			root2 = list.get(0);
		}
		Assertions.assertEquals(1, root1.getList().size());
		Assertions.assertEquals(1, root1.getList2().size());
		Assertions.assertEquals(3, root1.getList3().size());
		Assertions.assertEquals(2, root2.getList().size());
		Assertions.assertEquals(2, root2.getList2().size());
		Assertions.assertEquals(0, root2.getList3().size());
		for (SubEntity s : root1.getList())
			Assertions.assertEquals(root1, s.getParent());
		for (SubEntity s : root2.getList())
			Assertions.assertEquals(root2, s.getParent());
		for (SubEntity2 s : root1.getList2())
			Assertions.assertEquals(root1, s.getParent());
		for (SubEntity2 s : root2.getList2())
			Assertions.assertEquals(root2, s.getParent());
		for (SubEntity3 s : root1.getList3())
			Assertions.assertEquals(root1, s.getParent());
		for (SubEntity3 s : root2.getList3())
			Assertions.assertEquals(root2, s.getParent());
		Assertions.assertEquals(2, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(3, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(3, SelectQuery.from(SubEntity2.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(3, SelectQuery.from(SubEntity3.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(1, root1.getList3().stream().filter(s -> "3.1".equals(s.getSubValue())).findFirst().get().getVersion());
		Assertions.assertEquals(3, root1.getList3().stream().filter(s -> "3.2".equals(s.getSubValue())).findFirst().get().getVersion());
		Assertions.assertEquals(3, root1.getList3().stream().filter(s -> "3.3".equals(s.getSubValue())).findFirst().get().getVersion());

		// remove sub1_2 from root1 => no more SubEntity
		sub1_2 = root1.getList().stream().filter(s -> "1.2".equals(s.getSubValue())).findFirst().get();
		root1.getList().remove(sub1_2);
		// remove sub1_1 from root2 => remaining = sub1_3
		sub1_1 = root2.getList().stream().filter(s -> "1.1".equals(s.getSubValue())).findFirst().get();
		root2.getList().remove(sub1_1);
		// remove sub2_1 from root1 => no more SubEntity2
		sub2_1 = root1.getList2().stream().filter(s -> "2.1".equals(s.getSubValue())).findFirst().get();
		root1.getList2().remove(sub2_1);
		// remove sub2_3 from root2 => remaining = sub2_2
		sub2_3 = root2.getList2().stream().filter(s -> "2.3".equals(s.getSubValue())).findFirst().get();
		root2.getList2().remove(sub2_3);
		// remove sub3_1 from root1 => remaining = sub3_2 + sub3_3
		sub3_1 = root1.getList3().stream().filter(s -> "3.1".equals(s.getSubValue())).findFirst().get();
		root1.getList3().remove(sub3_1); // 3.1 version 2
		
		repo.saveAll(Arrays.asList(root1, root2)).collectList().block();
		list = repo.findAllFull().collectList().block();
		Assertions.assertEquals(2, list.size());
		if ("one".equals(list.get(0).getValue())) {
			root1 = list.get(0);
			root2 = list.get(1);
		} else {
			root1 = list.get(1);
			root2 = list.get(0);
		}
		Assertions.assertEquals(0, root1.getList().size());
		Assertions.assertEquals(0, root1.getList2().size());
		Assertions.assertEquals(2, root1.getList3().size());
		Assertions.assertEquals(1, root2.getList().size());
		Assertions.assertEquals(1, root2.getList2().size());
		Assertions.assertEquals(0, root2.getList3().size());
		Assertions.assertEquals(2, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		// not optional => only 1 SubEntity must remain
		Assertions.assertEquals(1, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
		// delete => only 1 SubEntity2 must remain
		Assertions.assertEquals(1, SelectQuery.from(SubEntity2.class, "entity").execute(lcClient).collectList().block().size());
		// set to null => the 3 SubEntity3 must remain
		List<SubEntity3> list3 = SelectQuery.from(SubEntity3.class, "entity").execute(lcClient).collectList().block();
		Assertions.assertEquals(3, list3.size());
		Assertions.assertEquals(2, list3.stream().filter(s -> "3.1".equals(s.getSubValue())).findFirst().get().getVersion());
		Assertions.assertEquals(3, list3.stream().filter(s -> "3.2".equals(s.getSubValue())).findFirst().get().getVersion());
		Assertions.assertEquals(3, list3.stream().filter(s -> "3.3".equals(s.getSubValue())).findFirst().get().getVersion());
	}
	
	@Test
	public void testDeleteWithLazyCollection() {
		RootEntity root1 = new RootEntity();
		root1.setValue("one");
		RootEntity root2 = new RootEntity();
		root2.setValue("two");
		
		SubEntity sub1_1 = new SubEntity();
		sub1_1.setSubValue("1.1");
		SubEntity sub1_2 = new SubEntity();
		sub1_2.setSubValue("1.2");
		
		SubEntity2 sub2_1 = new SubEntity2("2.1");
		SubEntity2 sub2_2 = new SubEntity2("2.2");
		
		SubEntity3 sub3_1 = new SubEntity3();
		sub3_1.setSubValue("3.1");
		SubEntity3 sub3_2 = new SubEntity3();
		sub3_2.setSubValue("3.2");
		
		root1.setList(Arrays.asList(sub1_1, sub1_2));
		root1.setList2(Arrays.asList(sub2_1));
		root1.setList3(Arrays.asList(sub3_1));

		root2.setList2(Arrays.asList(sub2_2));
		root2.setList3(Arrays.asList(sub3_2));
		
		repo.saveAll(Arrays.asList(root1, root2)).collectList().block();

		Assertions.assertEquals(2, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(2, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(2, SelectQuery.from(SubEntity2.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(2, SelectQuery.from(SubEntity3.class, "entity").execute(lcClient).collectList().block().size());
		
		root1 = repo.findByValue("one").blockFirst();
		Assertions.assertNull(root1.getList());
		repo.delete(root1).block();
		// root1 and its links must be removed, sub3_1 should remain with a null parent
		Assertions.assertEquals(1, SelectQuery.from(RootEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(0, SelectQuery.from(SubEntity.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(1, SelectQuery.from(SubEntity2.class, "entity").execute(lcClient).collectList().block().size());
		Assertions.assertEquals(2, SelectQuery.from(SubEntity3.class, "entity").execute(lcClient).collectList().block().size());
	}
	
	@Test
	public void testConditions() {
		RootEntity root1 = new RootEntity();
		root1.setValue("one");
		RootEntity root2 = new RootEntity();
		root2.setValue("two");
		RootEntity root3 = new RootEntity();
		root3.setValue("abocd");

		
		SubEntity sub1_1 = new SubEntity();
		sub1_1.setSubValue("1.1");
		SubEntity sub1_2 = new SubEntity();
		sub1_2.setSubValue("1.2");
		SubEntity sub1_3 = new SubEntity();
		sub1_3.setSubValue("1.3");
		
		SubEntity2 sub2_1 = new SubEntity2("2.1");
		SubEntity2 sub2_2 = new SubEntity2("2.2");
		
		SubEntity3 sub3_1 = new SubEntity3();
		sub3_1.setSubValue("3.1");
		SubEntity3 sub3_2 = new SubEntity3();
		sub3_2.setSubValue("3.2");
		SubEntity3 sub3_3 = new SubEntity3();
		sub3_3.setSubValue("3.3");
		
		root1.setList(Arrays.asList(sub1_1));
		root1.setList2(Arrays.asList(sub2_1));
		root1.setList3(Arrays.asList(sub3_1));

		root2.setList(Arrays.asList(sub1_3));
		root2.setList2(Arrays.asList(sub2_2));
		root2.setList3(Arrays.asList(sub3_2));
		
		root3.setList(Arrays.asList(sub1_2));
		root3.setList3(Arrays.asList(sub3_3));
		
		repo.saveAll(Arrays.asList(root1, root2, root3)).collectList().block();

		List<SubEntity> subs =
			SelectQuery.from(SubEntity.class, "sub1")
			.join("sub1", "parent", "root")
			.join("root", "list3", "sub3")
			.where(
				Criteria.property("sub3", "subValue").isNotNull()
				.and(Criteria.property("root", "value").like("%o%"))
				.and(Criteria.property("sub1", "subValue").like("%.2").or(Criteria.property("sub3", "subValue").is("3.1")))
			)
			.execute(lcClient)
			.collectList().block();
		Assertions.assertEquals(2, subs.size());
		Assertions.assertTrue(subs.stream().anyMatch(sub -> "1.1".equals(sub.getSubValue())));
		Assertions.assertTrue(subs.stream().anyMatch(sub -> "1.2".equals(sub.getSubValue())));
	}

}
