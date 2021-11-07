package net.lecousin.reactive.data.relational.postgres;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.lecousin.reactive.data.relational.configuration.LcReactiveDataRelationalConfiguration;

@Configuration
public class PostgresConfiguration extends LcReactiveDataRelationalConfiguration {

	@Bean
	@Override
	public PostgresSchemaDialect schemaDialect() {
		return new PostgresSchemaDialect();
	}
	
}
