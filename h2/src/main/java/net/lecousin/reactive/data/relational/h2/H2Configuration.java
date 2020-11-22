package net.lecousin.reactive.data.relational.h2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.lecousin.reactive.data.relational.configuration.LcReactiveDataRelationalConfiguration;
import net.lecousin.reactive.data.relational.mapping.LcReactiveDataAccessStrategy;

@Configuration
public abstract class H2Configuration extends LcReactiveDataRelationalConfiguration {

	@Bean
	@Override
	public H2SchemaGenerationDialect lcSchemaGenerationDialect(LcReactiveDataAccessStrategy dataAccess) {
		return new H2SchemaGenerationDialect(dataAccess);
	}
	
}
