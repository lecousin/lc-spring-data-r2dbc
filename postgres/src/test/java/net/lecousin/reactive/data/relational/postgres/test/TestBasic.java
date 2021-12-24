package net.lecousin.reactive.data.relational.postgres.test;

import org.springframework.test.context.ContextConfiguration;

import net.lecousin.reactive.data.relational.postgres.PostgresSchemaDialect;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;
import net.lecousin.reactive.data.relational.test.AbstractBasicTest;

@ContextConfiguration(classes = { PostgresTestConfiguration.class })
public class TestBasic extends AbstractBasicTest {

	@Override
	protected Class<? extends RelationalDatabaseSchemaDialect> expectedDialect() {
		return PostgresSchemaDialect.class;
	}
	
}
