package net.lecousin.reactive.data.relational.mysql.test.v8_0_17;

import org.springframework.test.context.ContextConfiguration;

import net.lecousin.reactive.data.relational.mysql.MySqlSchemaDialect;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;
import net.lecousin.reactive.data.relational.test.AbstractBasicTest;

@ContextConfiguration(classes = { MySql_v8_0_17_TestConfiguration.class })
public class TestBasic extends AbstractBasicTest {

	@Override
	protected Class<? extends RelationalDatabaseSchemaDialect> expectedDialect() {
		return MySqlSchemaDialect.class;
	}
	
}
