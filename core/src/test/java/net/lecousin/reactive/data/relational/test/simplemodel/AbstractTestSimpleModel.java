package net.lecousin.reactive.data.relational.test.simplemodel;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;
import net.lecousin.reactive.data.relational.test.AbstractLcReactiveDataRelationalTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
	private UUIDEntityRepository uuidRepo;
	
	@Autowired
	private EnumRepository enumRepo;
	
	@Autowired
	private TransactionalService transactionalService;
	
	@Override
	protected Collection<Class<?>> usedEntities() {
		LinkedList<Class<?>> entities = new LinkedList<>();
		Collections.addAll(entities,
			BooleanTypes.class,
			CharacterTypes.class,
			CompositeIdEntity.class,
			DateTypes.class,
			Entity1WithSequence.class,
			Entity2WithSequence.class,
			EnumEntity.class,
			NumericTypes.class,
			UpdatableProperties.class,
			UUIDEntity.class,
			VersionedEntity.class
		);
		if (lcClient.getSchemaDialect().isTimeZoneSupported())
			entities.add(DateTypesWithTimeZone.class);
		return entities;
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
		
		// test invalid custom query
		try {
			repoBool.findAllWithoutId().collectList().block();
			throw new AssertionError();
		} catch (MappingException e) {
			// ok
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
		Long id1 = e1.getId();
		Assertions.assertNotNull(id1);
		Long id2 = e2.getId();
		Assertions.assertNotNull(id2);
		
		Assertions.assertEquals(2, repoBool.findAllById(Arrays.asList(id1, id2)).collectList().block().size());
		Assertions.assertEquals(0, repoBool.findAllById(new ArrayList<>(0)).collectList().block().size());
		Assertions.assertEquals(0, repoBool.findAllById(Arrays.asList(123456789L)).collectList().block().size());

		repoBool.deleteAll(Arrays.asList(e1, e2)).block();
		list = repoBool.findAll().collectList().block();
		Assertions.assertEquals(0, list.size());
		Assertions.assertNull(e1.getId());
		Assertions.assertNull(e2.getId());
		
		Assertions.assertEquals(0, repoBool.findAllById(Arrays.asList(id1, id2)).collectList().block().size());
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
		
		List<NumericTypes> rows = repoNum.findByLong1(-7L).collectList().block();
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
	public void testStringFunctions() {
		CharacterTypes e1 = new CharacterTypes();
		e1.setStr(null);
		e1.setFixedLengthString("a");
		CharacterTypes e2 = new CharacterTypes();
		e2.setStr("Hello");
		e2.setFixedLengthString("b");
		CharacterTypes e3 = new CharacterTypes();
		e3.setStr("World");
		e3.setFixedLengthString("c");
		
		repoChars.saveAll(Arrays.asList(e1, e2, e3)).collectList().block();
		
		List<CharacterTypes> list = repoChars.findAll().collectList().block();
		Assertions.assertEquals(3, list.size());
		
		list = SelectQuery.from(CharacterTypes.class, "e").where(Criteria.property("e", "str").is("hello")).execute(lcClient).collectList().block();
		Assertions.assertEquals(0, list.size());
		
		list = SelectQuery.from(CharacterTypes.class, "e").where(Criteria.property("e", "str").toLowerCase().is("hello")).execute(lcClient).collectList().block();
		Assertions.assertEquals(1, list.size());
		Assertions.assertEquals("Hello", list.get(0).getStr());
		
		list = SelectQuery.from(CharacterTypes.class, "e").where(Criteria.property("e", "str").toUpperCase().is("hello")).execute(lcClient).collectList().block();
		Assertions.assertEquals(0, list.size());
		
		list = SelectQuery.from(CharacterTypes.class, "e").where(Criteria.property("e", "str").toUpperCase().is("HELLO")).execute(lcClient).collectList().block();
		Assertions.assertEquals(1, list.size());
		Assertions.assertEquals("Hello", list.get(0).getStr());
	}
	
	@Test
	public void testDates() {
		java.time.Instant instant = java.time.Instant.now();
		instant = instant.truncatedTo(ChronoUnit.MILLIS);
		long epoch = instant.toEpochMilli();
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(epoch);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1;
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		int millis = calendar.get(Calendar.MILLISECOND);
		int nano = millis * 1000000;
		
		DateTypes entity = new DateTypes();
		entity.setTimeInstant(instant);
		entity.setTimeLocalDate(java.time.LocalDate.of(year, month, day));
		entity.setTimeLocalTime(java.time.LocalTime.of(hour, minute, second, nano));
		entity.setTimeLocalDateTime(java.time.LocalDateTime.of(year, month, day, hour, minute, second, nano));
		entity.setTimeLocalDateTimeWithoutPrecision(java.time.LocalDateTime.of(year, month, day, hour, minute, second, nano));
		
		entity = repoDate.save(entity).block();
		
		entity = repoDate.findAll().blockFirst();
		Assertions.assertEquals(epoch, entity.getTimeInstant().toEpochMilli());
		Assertions.assertEquals(year, entity.getTimeLocalDate().getYear());
		Assertions.assertEquals(month, entity.getTimeLocalDate().getMonthValue());
		Assertions.assertEquals(day, entity.getTimeLocalDate().getDayOfMonth());
		Assertions.assertEquals(hour, entity.getTimeLocalTime().getHour());		
		Assertions.assertEquals(minute, entity.getTimeLocalTime().getMinute());		
		Assertions.assertEquals(second, entity.getTimeLocalTime().getSecond());		
		Assertions.assertEquals(nano, entity.getTimeLocalTime().getNano());
		Assertions.assertEquals(java.time.LocalDateTime.of(year, month, day, hour, minute, second, nano), entity.getTimeLocalDateTime());
		Assertions.assertEquals(java.time.LocalDateTime.of(year, month, day, hour, minute, second, nano), entity.getTimeLocalDateTimeWithoutPrecision());
		
		Assertions.assertNotNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").dateToYear().is(year)).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").dateToYear().is(year + 1)).execute(lcClient).blockFirst());
		Assertions.assertNotNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").dateToMonth().is(month)).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").dateToMonth().is(month + 1)).execute(lcClient).blockFirst());
		Assertions.assertNotNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").dateToDayOfMonth().is(calendar.get(Calendar.DAY_OF_MONTH))).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").dateToDayOfMonth().is(calendar.get(Calendar.DAY_OF_MONTH) + 1)).execute(lcClient).blockFirst());
		Assertions.assertNotNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").dateToDayOfYear().is(calendar.get(Calendar.DAY_OF_YEAR))).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").dateToDayOfYear().is(calendar.get(Calendar.DAY_OF_YEAR) + 1)).execute(lcClient).blockFirst());
		Assertions.assertNotNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").timeToHour().is(hour)).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").timeToHour().is(hour + 1)).execute(lcClient).blockFirst());
		Assertions.assertNotNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").timeToMinute().is(minute)).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").timeToMinute().is(minute + 1)).execute(lcClient).blockFirst());
		Assertions.assertNotNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").timeToSecond().is(second)).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").timeToSecond().is(second + 1)).execute(lcClient).blockFirst());
		calendar.setMinimalDaysInFirstWeek(4);
		calendar.setFirstDayOfWeek(Calendar.MONDAY);
		Assertions.assertNotNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").dateToIsoWeek().is(calendar.get(Calendar.WEEK_OF_YEAR))).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").dateToIsoWeek().is(calendar.get(Calendar.WEEK_OF_YEAR) + 1)).execute(lcClient).blockFirst());
		Map<Integer, Integer> isoDow = new HashMap<>();
		isoDow.put(Calendar.MONDAY, 1);
		isoDow.put(Calendar.TUESDAY, 2);
		isoDow.put(Calendar.WEDNESDAY, 3);
		isoDow.put(Calendar.THURSDAY, 4);
		isoDow.put(Calendar.FRIDAY, 5);
		isoDow.put(Calendar.SATURDAY, 6);
		isoDow.put(Calendar.SUNDAY, 7);
		Assertions.assertNotNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").dateToIsoDayOfWeek().is(isoDow.get(calendar.get(Calendar.DAY_OF_WEEK)))).execute(lcClient).blockFirst());
		Assertions.assertNull(SelectQuery.from(DateTypes.class, "e").where(Criteria.property("e", "timeInstant").dateToIsoDayOfWeek().is(isoDow.get(calendar.get(Calendar.DAY_OF_WEEK)) + 1)).execute(lcClient).blockFirst());
	}
	
	@Test
	public void testDatesWithTimzone() {
		Assumptions.assumeTrue(lcClient.getSchemaDialect().isTimeZoneSupported());
		java.time.Instant instant = java.time.Instant.now();
		instant = instant.truncatedTo(ChronoUnit.MILLIS);
		long epoch = instant.toEpochMilli();
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(epoch);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		int millis = calendar.get(Calendar.MILLISECOND);
		int nano = millis * 1000000;
		
		DateTypesWithTimeZone entity = new DateTypesWithTimeZone();
		entity.setTimeOffsetTime(java.time.OffsetTime.of(hour, minute, second, nano, ZoneOffset.ofHours(4)));
		entity.setTimeZonedDateTime(java.time.ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
		entity.setTimeOffsetTimeWithoutPrecision(java.time.OffsetTime.of(hour, minute, second, nano, ZoneOffset.ofHours(4)));
		
		entity = lcClient.save(entity).block();
		
		entity = SelectQuery.from(DateTypesWithTimeZone.class, "e").execute(lcClient).blockFirst();
		Assertions.assertEquals(java.time.OffsetTime.of(hour, minute, second, nano, ZoneOffset.ofHours(4)), entity.getTimeOffsetTime());
		Assertions.assertEquals(java.time.ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()).toEpochSecond(), entity.getTimeZonedDateTime().toEpochSecond());
		Assertions.assertEquals(java.time.OffsetTime.of(hour, minute, second, nano, ZoneOffset.ofHours(4)), entity.getTimeOffsetTimeWithoutPrecision());
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
		Assertions.assertEquals(java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(entity1.getCreation()), ZoneId.systemDefault()), entity1.getModification());
		Assertions.assertEquals(java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(entity2.getCreation()), ZoneId.systemDefault()), entity2.getModification());
		Assertions.assertEquals(entity1.getCreation(), entity1.getCreationInstant().toEpochMilli());
		Assertions.assertEquals(java.time.LocalDate.ofInstant(java.time.Instant.ofEpochMilli(entity1.getCreation()), ZoneId.systemDefault()), entity1.getCreationLocalDate());
		Assertions.assertEquals(java.time.LocalTime.ofInstant(java.time.Instant.ofEpochMilli(entity1.getCreation()), ZoneId.systemDefault()), entity1.getCreationLocalTime());
		Assertions.assertEquals(java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(entity1.getCreation()), ZoneId.systemDefault()), entity1.getCreationLocalDateTime());
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
		Assertions.assertEquals(java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(entity1.getCreation()), ZoneId.systemDefault()), entity1.getModification());
		Assertions.assertEquals(java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(entity2.getCreation()), ZoneId.systemDefault()), entity2.getModification());
		
		// update entity2 => version 2
		entity2.setStr("World !");
		try {
			Thread.sleep(500); // make sure updated timestamp is different from created timestamp
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
		Assertions.assertEquals(java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(entity1.getCreation()), ZoneId.systemDefault()), entity1.getModification());
		Assertions.assertTrue(entity2.getCreation() != null && entity2.getCreation() >= creationDateStart && entity2.getCreation() <= creationDateEnd, "Creation date: " + entity2.getCreation());
		Assertions.assertTrue(entity2.getCreation() < entity2.getModification().toInstant(ZoneOffset.UTC).toEpochMilli());
		
		
		list = repoVersion.findAll().collectList().block();
		entity1 = list.stream().filter(e -> id1.equals(e.getId())).findFirst().get();
		entity2 = list.stream().filter(e -> id2.equals(e.getId())).findFirst().get();
		Assertions.assertEquals(1, entity1.getVersion());
		Assertions.assertEquals("Hello", entity1.getStr());
		Assertions.assertEquals(2, entity2.getVersion());
		Assertions.assertEquals("World !", entity2.getStr());
		Assertions.assertTrue(entity1.getCreation() != null && entity1.getCreation() >= creationDateStart && entity1.getCreation() <= creationDateEnd, "Creation date: " + entity1.getCreation());
		Assertions.assertEquals(java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(entity1.getCreation()), ZoneId.systemDefault()), entity1.getModification());
		Assertions.assertTrue(entity2.getCreation() != null && entity2.getCreation() >= creationDateStart && entity2.getCreation() <= creationDateEnd, "Creation date: " + entity2.getCreation());
		Assertions.assertTrue(entity2.getCreation() < entity2.getModification().toInstant(ZoneOffset.UTC).toEpochMilli());
		
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
	public void testSequence() {
		Assumptions.assumeTrue(lcClient.getSchemaDialect().supportsSequence());
		
		Entity1WithSequence e = new Entity1WithSequence();
		e.setValue("test");
		e = lcClient.save(e).block();
		Assertions.assertNotNull(e.getId());
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
		
		UpdatableProperties e2 = new UpdatableProperties();
		e2.setId(1L);
		e2.setStr1("s1");
		e2.setStr2("s2");
		e2.setStr3("s3");
		e2.setStr4("s4");
		row = lcClient.getDataAccess().getOutboundRow(e2);
		Assertions.assertEquals(4, row.keySet().size()); // str4 must be excluded
	}
	
	@Test
	public void testUUID() {
		Map<Integer, UUID> uuids = new HashMap<>();
		for (int i = 0; i < 10; ++i)
			uuids.put(i, UUID.randomUUID());
		
		List<UUIDEntity> entities = lcClient.save(Flux.fromIterable(uuids.entrySet())
			.map(entry -> {
				UUIDEntity e = new UUIDEntity();
				e.setUuidNonKey(entry.getValue());
				e.setI(entry.getKey());
				return e;
			})
		).collectList().block();
		Assertions.assertEquals(10, entities.size());
		Map<Integer, UUID> keys = new HashMap<>();
		for (UUIDEntity e : entities) {
			Assertions.assertNotNull(e.getUuidKey());
			Assertions.assertFalse(keys.containsKey(e.getI()));
			Assertions.assertFalse(keys.containsValue(e.getUuidKey()));
			keys.put(e.getI(), e.getUuidKey());
			Assertions.assertEquals(uuids.get(e.getI()), e.getUuidNonKey());
		}
		
		entities = SelectQuery.from(UUIDEntity.class, "e").execute(lcClient).collectList().block();
		Assertions.assertEquals(10, entities.size());
		keys = new HashMap<>();
		for (UUIDEntity e : entities) {
			Assertions.assertNotNull(e.getUuidKey());
			Assertions.assertFalse(keys.containsKey(e.getI()));
			Assertions.assertFalse(keys.containsValue(e.getUuidKey()));
			keys.put(e.getI(), e.getUuidKey());
			Assertions.assertEquals(uuids.get(e.getI()), e.getUuidNonKey());
			UUID newUuid = UUID.randomUUID();
			e.setUuidNonKey(newUuid);
			uuids.put(e.getI(), newUuid);
		}
		lcClient.save(entities).collectList().block();
		
		entities = uuidRepo.findAll().collectList().block();
		Assertions.assertEquals(10, entities.size());
		keys = new HashMap<>();
		for (UUIDEntity e : entities) {
			Assertions.assertNotNull(e.getUuidKey());
			Assertions.assertFalse(keys.containsKey(e.getI()));
			Assertions.assertFalse(keys.containsValue(e.getUuidKey()));
			keys.put(e.getI(), e.getUuidKey());
			Assertions.assertEquals(uuids.get(e.getI()), e.getUuidNonKey());
		}
		
		Assertions.assertTrue(uuidRepo.existsById(entities.get(0).getUuidKey()).block());
		Assertions.assertTrue(uuidRepo.existsById(Mono.just(entities.get(0).getUuidKey())).block());
		Assertions.assertFalse(uuidRepo.existsById(UUID.randomUUID()).block());
		Assertions.assertFalse(uuidRepo.existsById(Mono.just(UUID.randomUUID())).block());

		Assertions.assertNotNull(uuidRepo.findById(entities.get(0).getUuidKey()).block());
		Assertions.assertNotNull(uuidRepo.findById(Mono.just(entities.get(0).getUuidKey())).block());
		Assertions.assertNull(uuidRepo.findById(UUID.randomUUID()).block());
		Assertions.assertNull(uuidRepo.findById(Mono.just(UUID.randomUUID())).block());

		entities = uuidRepo.findAllById(Flux.fromIterable(entities).map(UUIDEntity::getUuidKey)).collectList().block();
		Assertions.assertEquals(10, entities.size());
		entities = uuidRepo.findAllById(Flux.fromIterable(entities.subList(2, 5)).map(UUIDEntity::getUuidKey).concatWithValues(UUID.randomUUID())).collectList().block();
		Assertions.assertEquals(3, entities.size());
		entities = uuidRepo.findAll().collectList().block();
		Assertions.assertEquals(10, entities.size());
		
		entities = uuidRepo.findAllById(entities.stream().map(UUIDEntity::getUuidKey).collect(Collectors.toList())).collectList().block();
		Assertions.assertEquals(10, entities.size());
		List<UUID> idsList = new ArrayList<>(5);
		idsList.add(UUID.randomUUID());
		idsList.add(entities.get(1).getUuidKey());
		idsList.add(UUID.randomUUID());
		idsList.add(entities.get(6).getUuidKey());
		idsList.add(UUID.randomUUID());
		entities = uuidRepo.findAllById(idsList).collectList().block();
		Assertions.assertEquals(2, entities.size());
		
		entities = uuidRepo.findAll().collectList().block();
		Assertions.assertEquals(10, entities.size());
		lcClient.delete(entities).block();
		entities = SelectQuery.from(UUIDEntity.class, "e").execute(lcClient).collectList().block();
		Assertions.assertEquals(0, entities.size());
		
		entities = lcClient.save(Flux.fromIterable(uuids.entrySet())
			.map(entry -> {
				UUIDEntity e = new UUIDEntity();
				e.setUuidNonKey(entry.getValue());
				e.setI(entry.getKey());
				return e;
			})
		).collectList().block();
		Assertions.assertEquals(10, entities.size());

		uuidRepo.deleteAll().block();
		entities = SelectQuery.from(UUIDEntity.class, "e").execute(lcClient).collectList().block();
		Assertions.assertEquals(0, entities.size());
		
		entities = lcClient.save(Flux.fromIterable(uuids.entrySet())
			.map(entry -> {
				UUIDEntity e = new UUIDEntity();
				e.setUuidNonKey(entry.getValue());
				e.setI(entry.getKey());
				return e;
			})
		).collectList().block();
		Assertions.assertEquals(10, entities.size());

		uuidRepo.deleteById(Flux.fromIterable(entities).map(UUIDEntity::getUuidKey)).block();
		entities = SelectQuery.from(UUIDEntity.class, "e").execute(lcClient).collectList().block();
		Assertions.assertEquals(0, entities.size());
		
		entities = lcClient.save(Flux.fromIterable(uuids.entrySet())
			.map(entry -> {
				UUIDEntity e = new UUIDEntity();
				e.setUuidNonKey(entry.getValue());
				e.setI(entry.getKey());
				return e;
			})
		).collectList().block();
		Assertions.assertEquals(10, entities.size());

		uuidRepo.deleteAllById(entities.stream().map(UUIDEntity::getUuidKey).collect(Collectors.toList())).block();
		entities = SelectQuery.from(UUIDEntity.class, "e").execute(lcClient).collectList().block();
		Assertions.assertEquals(0, entities.size());
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
	
	@Test
	public void testInsertAndDeleteManyEntities() {
		final int nb = lcClient.getSchemaDialect().isMultipleInsertSupported() ? 1000000 : 1000;
		lcClient.save(Flux.range(0, nb)
			.map(i -> {
				BooleanTypes entity = new BooleanTypes();
				entity.setB1(null);
				entity.setB2(true);
				return entity;
			})
		).then().block();
		Assertions.assertEquals(nb, repoBool.count().block());
		
		repoBool.deleteAll().block();
		
		Assertions.assertEquals(0, repoBool.count().block());
	}
	
	@Test
	public void testDeleteById() {
		List<BooleanTypes> entities = lcClient.save(Flux.range(0, 10)
			.map(i -> {
				BooleanTypes entity = new BooleanTypes();
				entity.setB1(null);
				entity.setB2(true);
				return entity;
			})
		).collectList().block();
		Assertions.assertEquals(10, repoBool.count().block());
		int nb = 10;
		for (BooleanTypes entity : entities) {
			repoBool.deleteById(entity.getId()).block();
			Assertions.assertEquals(--nb, repoBool.count().block());
			Assertions.assertTrue(repoBool.findById(entity.getId()).blockOptional().isEmpty());
		}
		Assertions.assertEquals(0, repoBool.count().block());
		
		entities = lcClient.save(Flux.range(0, 10)
			.map(i -> {
				BooleanTypes entity = new BooleanTypes();
				entity.setB1(null);
				entity.setB2(true);
				return entity;
			})
		).collectList().block();
		Assertions.assertEquals(10, repoBool.count().block());
		
		repoBool.deleteById(Flux.fromIterable(entities).map(BooleanTypes::getId)).block();
		Assertions.assertEquals(0, repoBool.count().block());
	}
	
	@Test
	public void testCompositeId() {
		List<CompositeIdEntity> entities = lcClient.save(Flux.range(0, 10)
			.map(i -> {
				CompositeIdEntity entity = new CompositeIdEntity();
				entity.setId1(Long.valueOf(i / 2));
				entity.setId2("Test" + (i % 2));
				entity.setStr("entity" + i);
				return entity;
			})
		).collectList().block();
		Assertions.assertEquals(10, entities.size());
		Assertions.assertEquals(10, SelectQuery.from(CompositeIdEntity.class, "entity").executeCount(lcClient).block());
		
		entities = SelectQuery.from(CompositeIdEntity.class, "entity").execute(lcClient).collectList().block();
		Assertions.assertEquals(10, entities.size());
		for (CompositeIdEntity entity : entities) {
			Assertions.assertTrue(entity.getStr().startsWith("entity"));
			int i = Integer.parseInt(entity.getStr().substring(6));
			Assertions.assertEquals(Long.valueOf(i / 2), entity.getId1());
			Assertions.assertEquals("Test" + (i % 2), entity.getId2());
			entity.setStr("updated" + i);
		}
		lcClient.save(entities).collectList().block();
		entities = SelectQuery.from(CompositeIdEntity.class, "entity").execute(lcClient).collectList().block();
		Assertions.assertEquals(10, entities.size());
		for (CompositeIdEntity entity : entities) {
			Assertions.assertTrue(entity.getStr().startsWith("updated"));
			int i = Integer.parseInt(entity.getStr().substring(7));
			Assertions.assertEquals(Long.valueOf(i / 2), entity.getId1());
			Assertions.assertEquals("Test" + (i % 2), entity.getId2());
		}
		lcClient.delete(entities).block();
		entities = SelectQuery.from(CompositeIdEntity.class, "entity").execute(lcClient).collectList().block();
		Assertions.assertEquals(0, entities.size());
	}
	
	@Test
	public void testEnum() {
		// test 1 entity
		EnumEntity entity1 = new EnumEntity();
		entity1.setE1(EnumEntity.Enum1.V1);
		entity1.setI(1);
		enumRepo.save(entity1).block();
		
		entity1 = enumRepo.findAll().blockFirst();
		Assertions.assertEquals(1, entity1.getI());
		Assertions.assertEquals(EnumEntity.Enum1.V1, entity1.getE1());

		// add 2 more entities
		EnumEntity entity2 = new EnumEntity();
		entity2.setE1(EnumEntity.Enum1.V2);
		entity2.setI(2);
		EnumEntity entity3 = new EnumEntity();
		entity3.setE1(EnumEntity.Enum1.V3);
		entity3.setI(3);
		enumRepo.saveAll(Arrays.asList(entity2, entity3)).collectList().block();
		
		List<EnumEntity> list = enumRepo.findAll().collectList().block();
		Assertions.assertEquals(3, list.size());
		Set<Integer> done = new HashSet<>();
		for (EnumEntity e : list) {
			switch (e.getI()) {
			case 1: Assertions.assertEquals(EnumEntity.Enum1.V1, e.getE1()); break;
			case 2: Assertions.assertEquals(EnumEntity.Enum1.V2, e.getE1()); break;
			case 3: Assertions.assertEquals(EnumEntity.Enum1.V3, e.getE1()); break;
			default: throw new AssertionError("Unexpected value: " + e.getI());
			}
			Assertions.assertFalse(done.contains(Integer.valueOf(e.getI())));
			done.add(Integer.valueOf(e.getI()));
		}
		
		// test criteria
		list = SelectQuery.from(EnumEntity.class, "e").where(Criteria.property("e", "e1").is(EnumEntity.Enum1.V2)).execute(lcClient).collectList().block();
		Assertions.assertEquals(1, list.size());
		entity2 = list.get(0);
		Assertions.assertEquals(2, entity2.getI());
		
		// test update
		entity2.setE1(EnumEntity.Enum1.V3);
		entity2.setE2(EnumEntity.Enum1.V1);
		lcClient.save(entity2).block();
		
		// no more V2
		Assertions.assertEquals(0, SelectQuery.from(EnumEntity.class, "e").where(Criteria.property("e", "e1").is(EnumEntity.Enum1.V2)).executeCount(lcClient).block());
		// 2 V3
		Assertions.assertEquals(2, SelectQuery.from(EnumEntity.class, "e").where(Criteria.property("e", "e1").is(EnumEntity.Enum1.V3)).executeCount(lcClient).block());
		// still 1 V1
		Assertions.assertEquals(1, SelectQuery.from(EnumEntity.class, "e").where(Criteria.property("e", "e1").is(EnumEntity.Enum1.V1)).executeCount(lcClient).block());
	}
	
	@Test
	public void testNonSenseOperations() {
		BooleanTypes b = new BooleanTypes();
		lcClient.delete(b).block();
	}
	
}
