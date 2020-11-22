package net.lecousin.reactive.data.relational.mysql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.lecousin.reactive.data.relational.configuration.LcReactiveDataRelationalConfiguration;
import net.lecousin.reactive.data.relational.mapping.LcReactiveDataAccessStrategy;

@Configuration
public abstract class MySqlConfiguration extends LcReactiveDataRelationalConfiguration {

	@Bean
	@Override
	public MySqlSchemaGenerationDialect lcSchemaGenerationDialect(LcReactiveDataAccessStrategy dataAccess) {
		return new MySqlSchemaGenerationDialect(dataAccess);
	}
	
}
