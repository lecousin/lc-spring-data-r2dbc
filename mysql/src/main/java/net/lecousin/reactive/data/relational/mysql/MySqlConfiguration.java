package net.lecousin.reactive.data.relational.mysql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.lecousin.reactive.data.relational.configuration.LcReactiveDataRelationalConfiguration;

@Configuration
public class MySqlConfiguration extends LcReactiveDataRelationalConfiguration {

	@Bean
	@Override
	public MySqlSchemaDialect schemaDialect() {
		return new MySqlSchemaDialect();
	}
	
}
