package net.lecousin.reactive.data.relational.test;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.model.metadata.EntityStaticMetadata;
import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.repository.LcR2dbcEntityTemplate;
import net.lecousin.reactive.data.relational.schema.RelationalDatabaseSchema;
import net.lecousin.reactive.data.relational.test.arraycolumns.EntityWithArrays;
import net.lecousin.reactive.data.relational.test.arraycolumns.UpdateableCollectionProperties;
import net.lecousin.reactive.data.relational.test.simplemodel.DateTypesWithTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@DataR2dbcTest
@EnableAutoConfiguration
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractLcReactiveDataRelationalTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLcReactiveDataRelationalTest.class);
	
	@Autowired
	protected LcR2dbcEntityTemplate template;
	
	@Autowired
	protected DatabaseClient springClient;
	
	protected LcReactiveDataRelationalClient lcClient;
	
	@PostConstruct
	public void retrieveLcClient() {
		lcClient = template.getLcClient();
	}
	
	@BeforeEach
	public void logStartTestInfo(TestInfo testInfo) {
		LOGGER.info("Start of test {} ({}#{})", testInfo.getDisplayName(), testInfo.getTestClass().map(Class::getName).orElse(""), testInfo.getTestMethod().map(Method::getName).orElse(""));
	}
	
	@AfterEach
	public void logEndTestInfo(TestInfo testInfo) {
		LOGGER.info("End of test {} ({}#{})", testInfo.getDisplayName(), testInfo.getTestClass().map(Class::getName).orElse(""), testInfo.getTestMethod().map(Method::getName).orElse(""));
	}
	
	@BeforeEach
	public void initDatabase() {
		LOGGER.info("Initializing database");
		Collection<Class<?>> usedEntities = usedEntities();
		if (usedEntities == null)
			usedEntities = getAllCompatibleEntities();
		RelationalDatabaseSchema schema = lcClient.buildSchemaFromEntities(usedEntities);
		lcClient.dropCreateSchemaContent(schema).block();
		LOGGER.info("Database initialized");
	}
	
	protected Collection<Class<?>> getAllCompatibleEntities() {
		Collection<Class<?>> entities = new LinkedList<>(EntityStaticMetadata.getClasses());
		if (!lcClient.getSchemaDialect().isTimeZoneSupported())
			entities.removeAll(Set.of(DateTypesWithTimeZone.class));
		if (!lcClient.getSchemaDialect().isArrayColumnSupported())
			entities.removeAll(Set.of(EntityWithArrays.class, UpdateableCollectionProperties.class));
		return entities;
	}
	
	protected Collection<Class<?>> usedEntities() {
		return null;
	}

	protected static class ExpectedEntity<T> {
		private ExpectedValue<T, ?>[] expectedValues;
		
		@SafeVarargs
		public ExpectedEntity(ExpectedValue<T, ?>... expectedValues) {
			this.expectedValues = expectedValues;
		}
	}
	
	protected static class ExpectedValue<S, T> {
		private Function<S, T> supplier;
		private T expected;
		
		public ExpectedValue(Function<S, T> supplier, T expected) {
			this.supplier = supplier;
			this.expected = expected;
		}
	}
	
	@SafeVarargs
	protected final <T> void expectEntities(Class<T> type, ExpectedEntity<T>... expected) {
		SelectQuery<T> selectAll = SelectQuery.from(type, "entity");
		List<T> found =  selectAll.execute(template.getLcClient()).collectList().block();
		Assertions.assertEquals(expected.length, found.size());
		Long count = selectAll.executeCount(lcClient).block();
		Assertions.assertNotNull(count);
		Assertions.assertEquals(expected.length, count.intValue());
		ArrayList<ExpectedEntity<T>> expectedEntities = new ArrayList<>(expected.length);
		Collections.addAll(expectedEntities, expected);
		StringBuilder error = new StringBuilder();
		for (T entity : found) {
			boolean matchFound = false;
			StringBuilder valuesFound = new StringBuilder();
			boolean first = true;
			for (Iterator<ExpectedEntity<T>> it = expectedEntities.iterator(); it.hasNext(); ) {
				ExpectedEntity<T> expectedEntity = it.next();
				boolean match = true;
				for (ExpectedValue<T, ?> expectedValue : expectedEntity.expectedValues) {
					Object value = expectedValue.supplier.apply(entity);
					if (first)
						valuesFound.append('<').append(value).append('>');
					if (!Objects.equals(value, expectedValue.expected))
						match = false;
				}
				first = false;
				if (match) {
					it.remove();
					matchFound = true;
					break;
				}
			}
			if (!matchFound) {
				error.append("Unexpected ").append(type.getSimpleName()).append(" with values ").append(valuesFound);
				for (ExpectedEntity<T> expectedEntity : expectedEntities) {
					error.append("\n - ");
					for (ExpectedValue<T, ?> expectedValue : expectedEntity.expectedValues)
						error.append('<').append(expectedValue.expected).append('>');
				}
				error.append('\n');
			}
		}
		if (!expectedEntities.isEmpty()) {
			error.append("Remaining expected entities: ");
			for (ExpectedEntity<T> expectedEntity : expectedEntities) {
				error.append("\n - ");
				for (ExpectedValue<T, ?> expectedValue : expectedEntity.expectedValues)
					error.append('<').append(expectedValue.expected).append('>');
			}
			error.append('\n');
		}
		if (error.length() > 0)
			throw new AssertionError(error.toString());
	}
	
}
