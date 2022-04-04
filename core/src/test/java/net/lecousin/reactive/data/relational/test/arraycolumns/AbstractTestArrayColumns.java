package net.lecousin.reactive.data.relational.test.arraycolumns;

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

@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
public abstract class AbstractTestArrayColumns extends AbstractLcReactiveDataRelationalTest {

	@Autowired
	private EntityWithArraysRepository repo1;
	
	@Override
	protected Collection<Class<?>> usedEntities() {
		return Arrays.asList(EntityWithArrays.class);
	}
	
	@Test
	public void testEmptyArrays() {
		EntityWithArrays entity = new EntityWithArrays();
		repo1.save(entity).block();
		
		entity = repo1.findAll().blockFirst();
		Assertions.assertTrue(entity.getIntegers() == null || entity.getIntegers().length == 0);
		Assertions.assertTrue(entity.getPrimitiveIntegers() == null || entity.getPrimitiveIntegers().length == 0);
	}
	
	@Test
	public void testArraysWithOneElement() {
		EntityWithArrays entity = new EntityWithArrays();
		entity.setBooleans(new Boolean[] { Boolean.TRUE });
		entity.setPrimitiveBooleans(new boolean[] { false });
		entity.setBooleanList(Arrays.asList(Boolean.TRUE));
		entity.setShorts(new Short[] { 11 });
		entity.setPrimitiveShorts(new short[] { 111 });
		entity.setShortList(Arrays.asList(Short.valueOf((short)1111)));
		entity.setIntegers(new Integer[] { 51 });
		entity.setPrimitiveIntegers(new int[] { 61 });
		entity.setIntegerList(Arrays.asList(71));
		entity.setLongs(new Long[] { 22L });
		entity.setPrimitiveLongs(new long[] { 222L });
		entity.setLongList(Arrays.asList(2222L));
		entity.setFloats(new Float[] { 1.1f });
		entity.setPrimitiveFloats(new float[] { 2.2f });
		entity.setFloatList(Arrays.asList(Float.valueOf(1.2f)));
		entity.setDoubles(new Double[] { 3.3d });
		entity.setPrimitiveDoubles(new double[] { 4.4d });
		entity.setDoubleList(Arrays.asList(4.5d));
		entity.setStrings(new String[] { "test1" });
		entity.setStringList(Arrays.asList("test2"));
		repo1.save(entity).block();
		
		entity = repo1.findAll().blockFirst();
		
		Assertions.assertNotNull(entity.getBooleans());
		Assertions.assertEquals(1, entity.getBooleans().length);
		Assertions.assertEquals(Boolean.TRUE, entity.getBooleans()[0]);
		
		Assertions.assertNotNull(entity.getPrimitiveBooleans());
		Assertions.assertEquals(1, entity.getPrimitiveBooleans().length);
		Assertions.assertFalse(entity.getPrimitiveBooleans()[0]);
		
		Assertions.assertNotNull(entity.getBooleanList());
		Assertions.assertEquals(1, entity.getBooleanList().size());
		Assertions.assertEquals(Boolean.TRUE, entity.getBooleanList().get(0));
		
		Assertions.assertNotNull(entity.getShorts());
		Assertions.assertEquals(1, entity.getShorts().length);
		Assertions.assertEquals(Short.valueOf((short)11), entity.getShorts()[0]);
		
		Assertions.assertNotNull(entity.getPrimitiveShorts());
		Assertions.assertEquals(1, entity.getPrimitiveShorts().length);
		Assertions.assertEquals((short)111, entity.getPrimitiveShorts()[0]);
		
		Assertions.assertNotNull(entity.getShortList());
		Assertions.assertEquals(1, entity.getShortList().size());
		Assertions.assertEquals(Short.valueOf((short)1111), entity.getShortList().get(0));
		
		Assertions.assertNotNull(entity.getIntegers());
		Assertions.assertEquals(1, entity.getIntegers().length);
		Assertions.assertEquals(51, entity.getIntegers()[0]);

		Assertions.assertNotNull(entity.getPrimitiveIntegers());
		Assertions.assertEquals(1, entity.getPrimitiveIntegers().length);
		Assertions.assertEquals(61, entity.getPrimitiveIntegers()[0]);
		
		Assertions.assertNotNull(entity.getIntegerList());
		Assertions.assertEquals(1, entity.getIntegerList().size());
		Assertions.assertEquals(Integer.valueOf(71), entity.getIntegerList().get(0));
		
		Assertions.assertNotNull(entity.getLongs());
		Assertions.assertEquals(1, entity.getLongs().length);
		Assertions.assertEquals(22L, entity.getLongs()[0]);

		Assertions.assertNotNull(entity.getPrimitiveLongs());
		Assertions.assertEquals(1, entity.getPrimitiveLongs().length);
		Assertions.assertEquals(222L, entity.getPrimitiveLongs()[0]);
		
		Assertions.assertNotNull(entity.getLongList());
		Assertions.assertEquals(1, entity.getLongList().size());
		Assertions.assertEquals(Long.valueOf(2222L), entity.getLongList().get(0));
		
		Assertions.assertNotNull(entity.getFloats());
		Assertions.assertEquals(1, entity.getFloats().length);
		Assertions.assertEquals(1.1f, entity.getFloats()[0]);

		Assertions.assertNotNull(entity.getPrimitiveFloats());
		Assertions.assertEquals(1, entity.getPrimitiveFloats().length);
		Assertions.assertEquals(2.2f, entity.getPrimitiveFloats()[0]);
		
		Assertions.assertNotNull(entity.getFloatList());
		Assertions.assertEquals(1, entity.getFloatList().size());
		Assertions.assertEquals(Float.valueOf(1.2f), entity.getFloatList().get(0));
		
		Assertions.assertNotNull(entity.getDoubles());
		Assertions.assertEquals(1, entity.getDoubles().length);
		Assertions.assertEquals(3.3d, entity.getDoubles()[0]);

		Assertions.assertNotNull(entity.getPrimitiveDoubles());
		Assertions.assertEquals(1, entity.getPrimitiveDoubles().length);
		Assertions.assertEquals(4.4d, entity.getPrimitiveDoubles()[0]);
		
		Assertions.assertNotNull(entity.getDoubleList());
		Assertions.assertEquals(1, entity.getDoubleList().size());
		Assertions.assertEquals(Double.valueOf(4.5d), entity.getDoubleList().get(0));
		
		Assertions.assertNotNull(entity.getStrings());
		Assertions.assertEquals(1, entity.getStrings().length);
		Assertions.assertEquals("test1", entity.getStrings()[0]);

		Assertions.assertNotNull(entity.getStringList());
		Assertions.assertEquals(1, entity.getStringList().size());
		Assertions.assertEquals("test2", entity.getStringList().get(0));
		
		// update an element in the array
		entity.getIntegers()[0] = 12345;
		repo1.save(entity).block();
		entity = repo1.findAll().blockFirst();
		Assertions.assertNotNull(entity.getIntegers());
		Assertions.assertEquals(1, entity.getIntegers().length);
		Assertions.assertEquals(12345, entity.getIntegers()[0]);
	}
	
	@Test
	public void testArraysWithThreeElements() {
		EntityWithArrays entity = new EntityWithArrays();
		entity.setIntegers(new Integer[] { 10, 20, 30 });
		entity.setPrimitiveIntegers(new int[] { 100, 200, 300 });
		entity.setStrings(new String[] { "test1", "test2", "test3" });
		repo1.save(entity).block();
		
		entity = repo1.findAll().blockFirst();
		Assertions.assertNotNull(entity.getIntegers());
		Assertions.assertEquals(3, entity.getIntegers().length);
		Assertions.assertEquals(10, entity.getIntegers()[0]);
		Assertions.assertEquals(20, entity.getIntegers()[1]);
		Assertions.assertEquals(30, entity.getIntegers()[2]);
		Assertions.assertNotNull(entity.getPrimitiveIntegers());
		Assertions.assertEquals(3, entity.getPrimitiveIntegers().length);
		Assertions.assertEquals(100, entity.getPrimitiveIntegers()[0]);
		Assertions.assertEquals(200, entity.getPrimitiveIntegers()[1]);
		Assertions.assertEquals(300, entity.getPrimitiveIntegers()[2]);
		Assertions.assertEquals(3, entity.getStrings().length);
		Assertions.assertEquals("test1", entity.getStrings()[0]);
		Assertions.assertEquals("test2", entity.getStrings()[1]);
		Assertions.assertEquals("test3", entity.getStrings()[2]);
	}
	
	@Test
	public void testSearchEntityHavingAnArrayContaining() {
		EntityWithArrays entity1 = new EntityWithArrays();
		entity1.setIntegers(new Integer[] { 10, 20, 30 });
		EntityWithArrays entity2 = new EntityWithArrays();
		entity2.setIntegers(new Integer[] { 20, 40, 60 });
		repo1.saveAll(Arrays.asList(entity1, entity2)).collectList().block();
		
		List<EntityWithArrays> list = SelectQuery.from(EntityWithArrays.class, "e")
			.where(Criteria.property("e", "integers").arrayContains(10))
			.execute(lcClient)
			.collectList().block();
		Assertions.assertEquals(1, list.size());
		Assertions.assertEquals(10, list.get(0).getIntegers()[0]);
		
		list = SelectQuery.from(EntityWithArrays.class, "e")
			.where(Criteria.property("e", "integers").arrayContains(20))
			.execute(lcClient)
			.collectList().block();
		Assertions.assertEquals(2, list.size());
	}
}
