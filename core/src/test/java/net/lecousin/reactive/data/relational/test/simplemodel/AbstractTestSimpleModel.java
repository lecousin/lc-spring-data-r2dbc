package net.lecousin.reactive.data.relational.test.simplemodel;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import io.r2dbc.spi.Row;
import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;
import net.lecousin.reactive.data.relational.test.AbstractLcReactiveDataRelationalTest;

@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
@ComponentScan
public abstract class AbstractTestSimpleModel extends AbstractLcReactiveDataRelationalTest {

	@Autowired
	private BooleanTypesRepository repoBool;
	
	@Autowired
	private NumericTypesRepository repoNum;
	
	@Autowired
	private CharacterTypesRepository repoChars;
	
	@Autowired
	private VersionedEntityRepository repoVersion;
	
	@Autowired
	private DateTypesRepository repoDate;
	
	@Autowired
	private TransactionalService transactionalService;
	
	@Override
	protected Collection<Class<?>> usedEntities() {
		return Arrays.asList(
			BooleanTypes.class,
			CharacterTypes.class,
			DateTypes.class,
			Entity1WithSequence.class,
			Entity2WithSequence.class,
			NumericTypes.class,
			UpdatableProperties.class,
			UUIDEntity.class,
			VersionedEntity.class
		);
	}
	
	@Test
	public void testBooleans() {
		BooleanTypes e1 = new BooleanTypes();
		e1.setB1(null);
		e1.setB2(true);
		BooleanTypes e2 = new BooleanTypes();
		e2.setB1(Boolean.TRUE);
		e2.setB2(false);
		
		repoBool.saveAll(Arrays.asList(e1, e2)).collectList().block();
		
		List<BooleanTypes> list = repoBool.findAll().collectList().block();
		Assertions.assertEquals(2, list.size());
		for (BooleanTypes e : list) {
			if (e.isB2())
				Assertions.assertNull(e.getB1());
			else
				Assertions.assertTrue(e.getB1());
		}
		Assertions.assertNotNull(e1.getId());
		Assertions.assertNotNull(e2.getId());
		
		list = repoBool.page(0, 3).collectList().block();
		Assertions.assertEquals(2, list.size());
		list = repoBool.page(1, 3).collectList().block();
		Assertions.assertEquals(1, list.size());
		list = repoBool.page(2, 3).collectList().block();
		Assertions.assertEquals(0, list.size());
		list = repoBool.page(3, 3).collectList().block();
		Assertions.assertEquals(0, list.size());
		list = repoBool.page(4, 3).collectList().block();
		Assertions.assertEquals(0, list.size());
		
		e1.setB2(false);
		e2.setB1(null);
		repoBool.saveAll(Arrays.asList(e1, e2)).collectList().block();
		list = repoBool.findAll().collectList().block();
		Assertions.assertEquals(2, list.size());
		Assertions.assertNull(list.get(0).getB1());
		Assertions.assertFalse(list.get(0).isB2());
		Assertions.assertNull(list.get(1).getB1());
		Assertions.assertFalse(list.get(1).isB2());

		e1 = list.get(0);
		e2 = list.get(1);
		e1.setB1(null);
		e1.setB2(true);
		e2.setB1(Boolean.TRUE);
		e2.setB2(false);
		repoBool.saveAll(Arrays.asList(e1, e2)).collectList().block();
		list = repoBool.findAll().collectList().block();
		Assertions.assertEquals(2, list.size());
		for (BooleanTypes e : list) {
			if (e.isB2())
				Assertions.assertNull(e.getB1());
			else
				Assertions.assertTrue(e.getB1());
		}
		
		repoBool.delete(e2).block();
		list = repoBool.findAll().collectList().block();
		Assertions.assertEquals(1, list.size());
		Assertions.assertNull(list.get(0).getB1());
		Assertions.assertTrue(list.get(0).isB2());
		Assertions.assertNotNull(e1.getId());
		Assertions.assertNull(e2.getId());

		repoBool.save(e2).block();
		list = repoBool.findAll().collectList().block();
		Assertions.assertEquals(2, list.size());
		for (BooleanTypes e : list) {
			if (e.isB2())
				Assertions.assertNull(e.getB1());
			else
				Assertions.assertTrue(e.getB1());
		}
		Assertions.assertNotNull(e1.getId());
		Assertions.assertNotNull(e2.getId());

		repoBool.deleteAll(Arrays.asList(e1, e2)).block();
		list = repoBool.findAll().collectList().block();
		Assertions.assertEquals(0, list.size());
		Assertions.assertNull(e1.getId());
		Assertions.assertNull(e2.getId());
	}
	
	@Test
	public void testNumerics() {
		NumericTypes e1 = new NumericTypes();
		e1.setByte1((byte)51);
		e1.setByte2(null);
		e1.setShort1((short)52);
		e1.setShort2(null);
		e1.setInt_1(53);
		e1.setInt_2(null);
		e1.setLong1(54L);
		e1.setLong2(null);
		e1.setFloat1(1.01f);
		e1.setFloat2(null);
		e1.setDouble1(1.02d);
		e1.setDouble2(null);
		e1.setBigDec(null);
		
		NumericTypes e2 = new NumericTypes();
		e2.setByte1((byte)11);
		e2.setByte2(Byte.valueOf((byte)1));
		e2.setShort1((short)12);
		e2.setShort2(Short.valueOf((short)2));
		e2.setInt_1(13);
		e2.setInt_2(3);
		e2.setLong1(14L);
		e2.setLong2(4L);
		e2.setFloat1(0.01f);
		e2.setFloat2(1.1f);
		e2.setDouble1(0.02d);
		e2.setDouble2(1.2d);
		e2.setBigDec(BigDecimal.ONE);
		
		repoNum.saveAll(Arrays.asList(e1, e2)).collectList().block();
		List<NumericTypes> list = repoNum.findAll().collectList().block();
		Assertions.assertEquals(2, list.size());
		for (NumericTypes e : list) {
			if (e.getByte2() == null) {
				Assertions.assertEquals((byte)51, e.getByte1());
				Assertions.assertEquals((short)52, e.getShort1());
				Assertions.assertNull(e.getShort2());
				Assertions.assertEquals(53, e.getInt_1());
				Assertions.assertNull(e.getInt_2());
				Assertions.assertEquals(54L, e.getLong1());
				Assertions.assertNull(e.getLong2());
				Assertions.assertEquals(1.01f, e.getFloat1());
				Assertions.assertNull(e.getFloat2());
				Assertions.assertEquals(1.02d, e.getDouble1());
				Assertions.assertNull(e.getDouble2());
				Assertions.assertNull(e.getBigDec());
			} else {
				Assertions.assertEquals((byte)11, e.getByte1());
				Assertions.assertEquals((byte)1, e.getByte2());
				Assertions.assertEquals((short)12, e.getShort1());
				Assertions.assertEquals((short)2, e.getShort2());
				Assertions.assertEquals(13, e.getInt_1());
				Assertions.assertEquals(3, e.getInt_2());
				Assertions.assertEquals(14L, e.getLong1());
				Assertions.assertEquals(4L, e.getLong2());
				Assertions.assertEquals(0.01f, e.getFloat1());
				Assertions.assertEquals(1.1f, e.getFloat2());
				Assertions.assertEquals(0.02d, e.getDouble1());
				Assertions.assertEquals(1.2d, e.getDouble2());
				Assertions.assertEquals(1d, e.getBigDec().doubleValue());
			}
		}
		
		e1.setByte1((byte)-1);
		e1.setByte2(Byte.valueOf((byte)-2));
		e1.setShort1((short)-3);
		e1.setShort2(Short.valueOf((short)-4));
		e1.setInt_1(-5);
		e1.setInt_2(Integer.valueOf(-6));
		e1.setLong1(-7L);
		e1.setLong2(Long.valueOf(-8L));
		e1.setFloat1(-9.09f);
		e1.setFloat2(Float.valueOf(-10.10f));
		e1.setDouble1(-11.11d);
		e1.setDouble2(Double.valueOf(-12.12d));
		e1.setBigDec(BigDecimal.valueOf(-13.13d));
		
		e2.setByte1((byte)0);
		e2.setByte2(null);
		e2.setShort1((short)0);
		e2.setShort2(null);
		e2.setInt_1(0);
		e2.setInt_2(null);
		e2.setLong1(-1L);
		e2.setLong2(null);
		e2.setFloat1(0f);
		e2.setFloat2(null);
		e2.setDouble1(0d);
		e2.setDouble2(null);
		e2.setBigDec(null);
		
		repoNum.saveAll(Arrays.asList(e1, e2)).collectList().block();
		
		list = repoNum.findAll().collectList().block();
		Assertions.assertEquals(2, list.size());
		for (NumericTypes e : list) {
			if (e.getId() == e1.getId()) {
				Assertions.assertEquals((byte)-1, e.getByte1());
				Assertions.assertEquals((byte)-2, e.getByte2());
				Assertions.assertEquals((short)-3, e.getShort1());
				Assertions.assertEquals((short)-4, e.getShort2());
				Assertions.assertEquals(-5, e.getInt_1());
				Assertions.assertEquals(-6, e.getInt_2());
				Assertions.assertEquals(-7L, e.getLong1());
				Assertions.assertEquals(-8L, e.getLong2());
				Assertions.assertEquals(-9.09f, e.getFloat1());
				Assertions.assertEquals(-10.10f, e.getFloat2());
				Assertions.assertEquals(-11.11d, e.getDouble1());
				Assertions.assertEquals(-12.12d, e.getDouble2());
				Assertions.assertEquals(-13.13d, e.getBigDec().doubleValue());
			} else {
				Assertions.assertEquals(e2.getId(), e.getId());
				Assertions.assertEquals((byte)0, e.getByte1());
				Assertions.assertNull(e.getByte2());
				Assertions.assertEquals((short)0, e.getShort1());
				Assertions.assertNull(e.getShort2());
				Assertions.assertEquals(0, e.getInt_1());
				Assertions.assertNull(e.getInt_2());
				Assertions.assertEquals(-1L, e.getLong1());
				Assertions.assertNull(e.getLong2());
				Assertions.assertEquals(0f, e.getFloat1());
				Assertions.assertNull(e.getFloat2());
				Assertions.assertEquals(0d, e.getDouble1());
				Assertions.assertNull(e.getDouble2());
				Assertions.assertNull(e.getBigDec());
			}
		}
		
		List<Row> rows = repoNum.findByLong1(-7L).collectList().block();
		Assertions.assertEquals(1,  rows.size());
		
		list = repoNum.getAllOnlyWithIdAndLong1().collectList().block();
		Assertions.assertEquals(2, list.size());
		Assertions.assertNull(list.get(0).getBigDec());
		Assertions.assertNull(list.get(1).getBigDec());
		Assertions.assertNotEquals(0L, list.get(0).getLong1());
		Assertions.assertNotEquals(0L, list.get(1).getLong1());
		
		List<Long> long1List = repoNum.getAllLong1().collectList().block();
		Assertions.assertEquals(2, long1List.size());
		Assertions.assertTrue(long1List.contains(Long.valueOf(-7L)));
		Assertions.assertTrue(long1List.contains(Long.valueOf(-1L)));
		
		repoNum.deleteAll(list).block();
		Assertions.assertEquals(0, repoNum.findAll().collectList().block().size());
	}
	
	@Test
	public void testCharacters() {
		StringBuilder longString = new StringBuilder(4600);
		for (int i = 0; i < 4500; ++i)
			longString.append((char)('a' + (i % 20)));
		
		CharacterTypes e1 = new CharacterTypes();
		e1.setC1('p');
		e1.setC2(null);
		e1.setStr(null);
		e1.setChars(null);
		e1.setFixedLengthString("abc");
		e1.setLongString(longString.toString());
		
		CharacterTypes e2 = new CharacterTypes();
		e2.setC1('\n');
		e2.setC2(Character.valueOf((char)0));
		e2.setStr("Hello");
		e2.setChars(new char[] { 'W', 'o', 'r', 'l', 'd' });
		e2.setFixedLengthString("abcde");
		e2.setLongString(null);
		
		repoChars.saveAll(Arrays.asList(e1, e2)).collectList().block();
		List<CharacterTypes> list = repoChars.findAll().collectList().block();
		Assertions.assertEquals(2, list.size());
		for (CharacterTypes e : list) {
			if (e.getC2() == null) {
				Assertions.assertEquals('p', e.getC1());
				Assertions.assertNull(e.getStr());
				Assertions.assertNull(e.getChars());
				Assertions.assertEquals("abc  ", e.getFixedLengthString());
				Assertions.assertEquals(longString.toString(), e.getLongString());
			} else {
				Assertions.assertEquals('\n', e.getC1());
				Assertions.assertEquals((char)0, e.getC2().charValue());
				Assertions.assertEquals("Hello", e.getStr());
				Assertions.assertEquals("World", new String(e.getChars()));
				Assertions.assertEquals("abcde", e.getFixedLengthString());
				Assertions.assertNull(e.getLongString());
			}
		}

		e1.setC1('\t');
		e1.setC2(Character.valueOf('x'));
		e1.setStr("Test");
		e1.setChars(new char[] { 'a', 'b', 'c' });

		e2.setC1('1');
		e2.setC2(null);
		e2.setStr(null);
		e2.setChars(null);
		
		repoChars.saveAll(Arrays.asList(e1, e2)).collectList().block();
		
		list = repoChars.findAll().collectList().block();
		Assertions.assertEquals(2, list.size());
		for (CharacterTypes e : list) {
			if (e.getId() == e1.getId()) {
				Assertions.assertEquals('\t', e.getC1());
				Assertions.assertEquals('x', e.getC2().charValue());
				Assertions.assertEquals("Test", e.getStr());
				Assertions.assertEquals("abc", new String(e.getChars()));
			} else {
				Assertions.assertEquals('1', e.getC1());
				Assertions.assertNull(e.getC2());
				Assertions.assertNull(e.getStr());
				Assertions.assertNull(e.getChars());
			}
		}
		
		repoChars.deleteAll(list).block();
		Assertions.assertEquals(0, repoChars.findAll().collectList().block().size());
	}
	
	@Test
	public void testDates() {
		java.time.Instant instant = java.time.Instant.now();
		long epoch = instant.toEpochMilli();
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(epoch);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1;
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		//int millis = calendar.get(Calendar.MILLISECOND);
		int nano = instant.get(ChronoField.NANO_OF_SECOND);
		
		DateTypes entity = new DateTypes();
		entity.setTimeInstant(instant);
		entity.setTimeLocalDate(java.time.LocalDate.of(year, month, day));
		entity.setTimeLocalTime(java.time.LocalTime.of(hour, minute, second, nano));
		entity.setTimeOffsetTime(java.time.OffsetTime.of(hour, minute, second, nano, ZoneOffset.ofHours(4)));
		entity.setTimeLocalDateTime(java.time.LocalDateTime.of(year, month, day, hour, minute, second, nano));
		entity.setTimeZonedDateTime(java.time.ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
		
		entity = repoDate.save(entity).block();
		
		entity = repoDate.findAll().blockFirst();
	}

	@Test
	public void testVersion() {
		VersionedEntity entity1 = new VersionedEntity();
		entity1.setStr("Hello");
		VersionedEntity entity2 = new VersionedEntity();
		entity2.setStr("World");
		entity2.setVersion(10L);
		
		long creationDateStart = System.currentTimeMillis();
		List<VersionedEntity> list = repoVersion.saveAll(Arrays.asList(entity1, entity2)).collectList().block();
		long creationDateEnd = System.currentTimeMillis();
		Assertions.assertEquals(2, list.size());
		entity1 = list.get(0);
		entity2 = list.get(1);
		Assertions.assertNotNull(entity1.getId());
		Assertions.assertEquals(1, entity1.getVersion());
		Assertions.assertEquals("Hello", entity1.getStr());
		Assertions.assertNotNull(entity2.getId());
		Assertions.assertEquals(1, entity2.getVersion());
		Assertions.assertEquals("World", entity2.getStr());
		Assertions.assertTrue(entity1.getCreation() != null && entity1.getCreation() >= creationDateStart && entity1.getCreation() <= creationDateEnd, "Creation date: " + entity1.getCreation());
		Assertions.assertTrue(entity2.getCreation() != null && entity2.getCreation() >= creationDateStart && entity2.getCreation() <= creationDateEnd, "Creation date: " + entity2.getCreation());
		Assertions.assertEquals(entity1.getCreation(), entity1.getModification().toInstant().toEpochMilli());
		Assertions.assertEquals(entity2.getCreation(), entity2.getModification().toInstant().toEpochMilli());
		Assertions.assertEquals(entity1.getCreation(), entity1.getCreationInstant().toEpochMilli());
		Assertions.assertEquals(java.time.LocalDate.ofInstant(java.time.Instant.ofEpochMilli(entity1.getCreation()), ZoneId.systemDefault()), entity1.getCreationLocalDate());
		Assertions.assertEquals(java.time.LocalTime.ofInstant(java.time.Instant.ofEpochMilli(entity1.getCreation()), ZoneId.systemDefault()), entity1.getCreationLocalTime());
		Assertions.assertEquals(java.time.OffsetTime.ofInstant(java.time.Instant.ofEpochMilli(entity1.getCreation()), ZoneId.systemDefault()), entity1.getCreationOffsetTime());
		Assertions.assertEquals(java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(entity1.getCreation()), ZoneId.systemDefault()), entity1.getCreationLocalDateTime());
		Assertions.assertEquals(java.time.ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(entity1.getCreation()), ZoneId.systemDefault()), entity1.getCreationZonedDateTime());
		final Long id1 = entity1.getId();
		final Long id2 = entity2.getId();
		
		list = repoVersion.findAll().collectList().block();
		entity1 = list.stream().filter(e -> id1.equals(e.getId())).findFirst().get();
		entity2 = list.stream().filter(e -> id2.equals(e.getId())).findFirst().get();
		Assertions.assertEquals(1, entity1.getVersion());
		Assertions.assertEquals("Hello", entity1.getStr());
		Assertions.assertEquals(1, entity2.getVersion());
		Assertions.assertEquals("World", entity2.getStr());
		Assertions.assertTrue(entity1.getCreation() != null && entity1.getCreation() >= creationDateStart && entity1.getCreation() <= creationDateEnd, "Creation date: " + entity1.getCreation());
		Assertions.assertTrue(entity2.getCreation() != null && entity2.getCreation() >= creationDateStart && entity2.getCreation() <= creationDateEnd, "Creation date: " + entity2.getCreation());
		Assertions.assertEquals(entity1.getCreation(), entity1.getModification().toInstant().toEpochMilli());
		Assertions.assertEquals(entity2.getCreation(), entity2.getModification().toInstant().toEpochMilli());
		
		// update entity2 => version 2
		entity2.setStr("World !");
		while (System.currentTimeMillis() == creationDateStart)
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		list = repoVersion.saveAll(Arrays.asList(entity1, entity2)).collectList().block();
		entity1 = list.stream().filter(e -> id1.equals(e.getId())).findFirst().get();
		entity2 = list.stream().filter(e -> id2.equals(e.getId())).findFirst().get();
		Assertions.assertEquals(1, entity1.getVersion());
		Assertions.assertEquals("Hello", entity1.getStr());
		Assertions.assertEquals(2, entity2.getVersion());
		Assertions.assertEquals("World !", entity2.getStr());
		Assertions.assertTrue(entity1.getCreation() != null && entity1.getCreation() >= creationDateStart && entity1.getCreation() <= creationDateEnd, "Creation date: " + entity1.getCreation());
		Assertions.assertEquals(entity1.getCreation(), entity1.getModification().toInstant().toEpochMilli());
		Assertions.assertTrue(entity2.getCreation() != null && entity2.getCreation() >= creationDateStart && entity2.getCreation() <= creationDateEnd, "Creation date: " + entity2.getCreation());
		Assertions.assertTrue(entity1.getCreation() == entity1.getModification().toInstant().toEpochMilli());
		Assertions.assertTrue(entity2.getCreation() < entity2.getModification().toInstant().toEpochMilli());
		
		
		list = repoVersion.findAll().collectList().block();
		entity1 = list.stream().filter(e -> id1.equals(e.getId())).findFirst().get();
		entity2 = list.stream().filter(e -> id2.equals(e.getId())).findFirst().get();
		Assertions.assertEquals(1, entity1.getVersion());
		Assertions.assertEquals("Hello", entity1.getStr());
		Assertions.assertEquals(2, entity2.getVersion());
		Assertions.assertEquals("World !", entity2.getStr());
		Assertions.assertTrue(entity1.getCreation() != null && entity1.getCreation() >= creationDateStart && entity1.getCreation() <= creationDateEnd, "Creation date: " + entity1.getCreation());
		Assertions.assertEquals(entity1.getCreation(), entity1.getModification().toInstant().toEpochMilli());
		Assertions.assertTrue(entity2.getCreation() != null && entity2.getCreation() >= creationDateStart && entity2.getCreation() <= creationDateEnd, "Creation date: " + entity2.getCreation());
		Assertions.assertTrue(entity2.getCreation() < entity2.getModification().toInstant().toEpochMilli());
		
		// save entity2 with version 1 => error
		entity2.setVersion(1L);
		entity2.setStr("World");
		try {
			list = repoVersion.saveAll(Arrays.asList(entity1, entity2)).collectList().block();
			throw new AssertionError("Error expected when saving entity with wrong version");
		} catch (OptimisticLockingFailureException e) {
			// expected
		}
	}
	
	@Test
	public void testCriteria() {
		NumericTypes e1 = new NumericTypes();
		e1.setByte1((byte)51);
		e1.setByte2(null);
		e1.setShort1((short)52);
		e1.setShort2(null);
		e1.setInt_1(13);
		e1.setInt_2(null);
		e1.setLong1(54L);
		e1.setLong2(null);
		e1.setFloat1(1.01f);
		e1.setFloat2(null);
		e1.setDouble1(1.02d);
		e1.setDouble2(null);
		e1.setBigDec(null);
		
		NumericTypes e2 = new NumericTypes();
		e2.setByte1((byte)11);
		e2.setByte2(Byte.valueOf((byte)1));
		e2.setShort1((short)12);
		e2.setShort2(Short.valueOf((short)2));
		e2.setInt_1(13);
		e2.setInt_2(1);
		e2.setLong1(14L);
		e2.setLong2(4L);
		e2.setFloat1(0.01f);
		e2.setFloat2(0.01f);
		e2.setDouble1(0.02d);
		e2.setDouble2(1.2d);
		e2.setBigDec(null);
		
		repoNum.saveAll(Arrays.asList(e1, e2)).collectList().block();
		
		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte1").is(Byte.valueOf((byte)51))).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte1").is(Byte.valueOf((byte)52))).execute(lcClient).blockFirst());
		
		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "float1").is("e", "float2")).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte2").is("e", "short1")).execute(lcClient).blockFirst());
		
		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte1").isNot(Byte.valueOf((byte)51))).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "int_1").isNot(Integer.valueOf(13))).execute(lcClient).blockFirst());
		
		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte2").isNot("e", "short2")).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte2").isNot("e", "int_2")).execute(lcClient).blockFirst());

		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte1").greaterThan(Byte.valueOf((byte)20))).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "int_1").greaterThan(Integer.valueOf(13))).execute(lcClient).blockFirst());

		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte1").greaterThan("e", "int_1")).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "int_2").greaterThan("e", "byte1")).execute(lcClient).blockFirst());

		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte1").greaterOrEqualTo(Byte.valueOf((byte)51))).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "int_1").greaterOrEqualTo(Integer.valueOf(14))).execute(lcClient).blockFirst());

		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte1").greaterOrEqualTo("e", "int_1")).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "int_2").greaterOrEqualTo("e", "byte1")).execute(lcClient).blockFirst());

		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte1").lessThan(Byte.valueOf((byte)51))).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "int_1").lessThan(Integer.valueOf(0))).execute(lcClient).blockFirst());

		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte1").lessThan("e", "int_1")).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "long1").lessThan("e", "byte1")).execute(lcClient).blockFirst());

		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte1").lessOrEqualTo(Byte.valueOf((byte)11))).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "int_1").lessOrEqualTo(Integer.valueOf(0))).execute(lcClient).blockFirst());

		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte1").lessOrEqualTo("e", "int_1")).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "long1").lessOrEqualTo("e", "byte1")).execute(lcClient).blockFirst());

		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte2").isNull()).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte1").isNull()).execute(lcClient).blockFirst());

		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "byte2").isNotNull()).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "bigDec").isNotNull()).execute(lcClient).blockFirst());

		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "int_2").in(Arrays.asList(1, 2, 3))).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "int_2").in(Arrays.asList(40, 50, 60))).execute(lcClient).blockFirst());

		Assertions.assertNotNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "int_2").notIn(Arrays.asList(2, 3, 4))).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "int_1").notIn(Arrays.asList(12, 13, 14))).execute(lcClient).blockFirst());
		
		// invalid criteria
		try {
			SelectQuery.from(NumericTypes.class, "e").where(Criteria.property("e", "int_1").is(Arrays.asList(12, 13, 14))).execute(lcClient).blockFirst();
			throw new AssertionError();
		} catch (Exception e) {
			// ok
		}
		try {
			Criteria.PropertyOperation c = new Criteria.PropertyOperation(Criteria.property("e", "int_1"), Criteria.PropertyOperator.IN, "wrong");
			SelectQuery.from(NumericTypes.class, "e").where(c).execute(lcClient).blockFirst();
			throw new AssertionError();
		} catch (Exception e) {
			// ok
		}
		
		
		CharacterTypes c = new CharacterTypes();
		c.setStr("Hello World");
		c.setLongString("Hello World");
		c.setFixedLengthString("Hello");
		repoChars.save(c).block();
		
		Assertions.assertNotNull(SelectQuery.from(CharacterTypes.class, "e").where(Criteria.property("e", "str").like("%lo%")).execute(lcClient).blockFirst());
		Assertions.assertNotNull(SelectQuery.from(CharacterTypes.class, "e").where(Criteria.property("e", "str").like("Hello%")).execute(lcClient).blockFirst());
		Assertions.assertNotNull(SelectQuery.from(CharacterTypes.class, "e").where(Criteria.property("e", "str").like("%World")).execute(lcClient).blockFirst());
		Assertions.assertNotNull(SelectQuery.from(CharacterTypes.class, "e").where(Criteria.property("e", "str").like("%o%")).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(CharacterTypes.class, "e").where(Criteria.property("e", "str").like("%la%")).execute(lcClient).blockFirst());
		Assertions.assertNotNull(SelectQuery.from(CharacterTypes.class, "e").where(Criteria.property("e", "str").notLike("%la%")).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(CharacterTypes.class, "e").where(Criteria.property("e", "str").like("e", "fixedLengthString")).execute(lcClient).blockFirst());
		Assertions.assertNotNull(SelectQuery.from(CharacterTypes.class, "e").where(Criteria.property("e", "str").notLike("e", "fixedLengthString")).execute(lcClient).blockFirst());
	}
	
	@Test
	public void testUpdatableProperties() {
		UpdatableProperties entity = new UpdatableProperties();
		entity.setStr1("1.1");
		entity.setStr2("2.1");
		entity.setStr3("3.1");
		entity.setStr4("4.1");
		
		entity = lcClient.save(entity).block();
		Assertions.assertEquals("1.1", entity.getStr1());
		Assertions.assertEquals("2.1", entity.getStr2());
		Assertions.assertEquals("3.1", entity.getStr3());
		Assertions.assertEquals("4.1", entity.getStr4());
		long id = entity.getId();
		
		entity.setId(10L);
		entity.setStr1("1.2");
		entity.setStr2("2.2");
		entity.setStr3("3.2");
		entity.setStr4("4.2");
		
		entity = lcClient.save(entity).block();
		Assertions.assertEquals(id, entity.getId());
		Assertions.assertEquals("1.2", entity.getStr1());
		Assertions.assertEquals("2.2", entity.getStr2());
		Assertions.assertEquals("3.1", entity.getStr3());
		Assertions.assertEquals("4.1", entity.getStr4());

		entity = SelectQuery.from(UpdatableProperties.class, "entity").execute(lcClient).blockFirst();
		Assertions.assertEquals(id, entity.getId());
		Assertions.assertEquals("1.2", entity.getStr1());
		Assertions.assertEquals("2.2", entity.getStr2());
		Assertions.assertEquals("3.1", entity.getStr3());
		Assertions.assertEquals("4.1", entity.getStr4());
	}
	
	@Test
	public void test2EntitiesWithSameSequence() {
		Assumptions.assumeTrue(lcClient.getSchemaDialect().supportsSequence());
		
		Entity1WithSequence e1_1 = new Entity1WithSequence();
		e1_1.setValue("1.1");
		Entity1WithSequence e1_2 = new Entity1WithSequence();
		e1_2.setValue("1.2");
		Entity2WithSequence e2_1 = new Entity2WithSequence();
		e2_1.setValue("2.1");
		Entity2WithSequence e2_2 = new Entity2WithSequence();
		e2_2.setValue("2.2");
		
		List<Entity1WithSequence> list1 = lcClient.save(Arrays.asList(e1_1, e1_2)).collectList().block();
		Assertions.assertEquals(2, list1.size());
		e1_1 = list1.stream().filter(e -> "1.1".equals(e.getValue())).findFirst().get();
		e1_2 = list1.stream().filter(e -> "1.2".equals(e.getValue())).findFirst().get();
		Assertions.assertTrue(e1_1.getId() < 3);
		Assertions.assertTrue(e1_2.getId() < 3);
		if (e1_1.getId() == 1)
			Assertions.assertEquals(2, e1_2.getId());
		else if (e1_2.getId() == 1)
			Assertions.assertEquals(2, e1_1.getId());
		else
			throw new AssertionError();
		
		List<Entity2WithSequence> list2 = lcClient.save(Arrays.asList(e2_1, e2_2)).collectList().block();
		Assertions.assertEquals(2, list2.size());
		e2_1 = list2.stream().filter(e -> "2.1".equals(e.getValue())).findFirst().get();
		e2_2 = list2.stream().filter(e -> "2.2".equals(e.getValue())).findFirst().get();
		Assertions.assertTrue(e2_1.getId() < 5);
		Assertions.assertTrue(e2_2.getId() < 5);
		if (e2_1.getId() == 3)
			Assertions.assertEquals(4, e2_2.getId());
		else if (e2_2.getId() == 3)
			Assertions.assertEquals(4, e2_1.getId());
		else
			throw new AssertionError();
	}
	
	@Test
	public void testSpringClient() {
		BooleanTypes e = new BooleanTypes();
		e.setB1(Boolean.TRUE);
		e.setB2(false);
		OutboundRow row = lcClient.getDataAccess().getOutboundRow(e);
		Assertions.assertEquals(3, row.keySet().size());
	}
	
	@Test
	public void testUUID() {
		UUIDEntity e = new UUIDEntity();
		e.setUuidNonKey(UUID.randomUUID());
		
		e = lcClient.save(e).block();
		Assertions.assertNotNull(e.getUuidKey());
	}
	
	@Test
	public void testTransactions() {
		CharacterTypes e = transactionalService.createCorrectEntity().block();
		Assertions.assertNotNull(e);
		Assertions.assertNotNull(e.getId());
		e = repoChars.findAll().blockFirst();
		Assertions.assertNotNull(e);
		Assertions.assertNotNull(e.getId());
		transactionalService.deleteEntity(e).block();
		Assertions.assertEquals(0, repoChars.findAll().collectList().block().size());
		
		try {
			transactionalService.createCorrectEntityThenInvalidEntity().collectList().block();
			throw new AssertionError();
		} catch (Exception err) {
			// ok
		}
		Assertions.assertEquals(0, repoChars.findAll().collectList().block().size());
	}
}
