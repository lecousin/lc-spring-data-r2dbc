package net.lecousin.reactive.data.relational.postgres;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.lecousin.reactive.data.relational.configuration.LcReactiveDataRelationalConfiguration;
import net.lecousin.reactive.data.relational.mapping.LcReactiveDataAccessStrategy;

@Configuration
public abstract class PostgresConfiguration extends LcReactiveDataRelationalConfiguration {

	@Bean
	@Override
	public PostgresSchemaGenerationDialect lcSchemaGenerationDialect(LcReactiveDataAccessStrategy dataAccess) {
		return new PostgresSchemaGenerationDialect(dataAccess);
	}
	
}
