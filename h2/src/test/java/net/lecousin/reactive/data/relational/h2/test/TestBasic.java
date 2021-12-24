package net.lecousin.reactive.data.relational.h2.test;

import org.springframework.test.context.ContextConfiguration;

import net.lecousin.reactive.data.relational.h2.H2SchemaDialect;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;
import net.lecousin.reactive.data.relational.test.AbstractBasicTest;

@ContextConfiguration(classes = { H2TestConfiguration.class })
public class TestBasic extends AbstractBasicTest {

	@Override
	protected Class<? extends RelationalDatabaseSchemaDialect> expectedDialect() {
		return H2SchemaDialect.class;
	}
	
}
