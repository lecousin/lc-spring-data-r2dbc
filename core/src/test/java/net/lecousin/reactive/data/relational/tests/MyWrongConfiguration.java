package net.lecousin.reactive.data.relational.tests;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;

import net.lecousin.reactive.data.relational.configuration.LcReactiveDataRelationalConfiguration;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;

@Configuration
public class MyWrongConfiguration extends LcReactiveDataRelationalConfiguration {

	@Override
	public RelationalDatabaseSchemaDialect schemaDialect() {
		return new RelationalDatabaseSchemaDialect() {
			@Override
			public boolean isCompatible(R2dbcDialect r2dbcDialect) {
				return true;
			}
		};
	}
	
}