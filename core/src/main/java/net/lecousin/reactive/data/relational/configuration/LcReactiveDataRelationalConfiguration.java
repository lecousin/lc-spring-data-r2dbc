/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lecousin.reactive.data.relational.configuration;

import java.util.Optional;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.util.Assert;

import io.r2dbc.spi.ConnectionFactory;
import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.mapping.LcMappingR2dbcConverter;
import net.lecousin.reactive.data.relational.mapping.LcR2dbcMappingContext;
import net.lecousin.reactive.data.relational.mapping.LcReactiveDataAccessStrategy;
import net.lecousin.reactive.data.relational.repository.LcR2dbcEntityTemplate;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;

/**
 * Configure R2DBC spring data extended by lc-spring-data-r2dbc.
 * 
 * @author Guillaume Le Cousin
 */
@Configuration
public abstract class LcReactiveDataRelationalConfiguration extends AbstractR2dbcConfiguration {

	private static final String CONNECTION_FACTORY_BEAN_NAME = "connectionFactory";
	
	protected @Nullable ApplicationContext context;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.context = applicationContext;
		super.setApplicationContext(applicationContext);
	}
	
	public abstract RelationalDatabaseSchemaDialect schemaDialect();
	
	@Bean
	public LcReactiveDataRelationalClient getLcClient(DatabaseClient databaseClient, ReactiveDataAccessStrategy dataAccessStrategy) {
		return new LcReactiveDataRelationalClient(
			databaseClient,
			(MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty>) dataAccessStrategy.getConverter().getMappingContext(),
			schemaDialect(),
			(LcReactiveDataAccessStrategy)dataAccessStrategy,
			(LcMappingR2dbcConverter) dataAccessStrategy.getConverter()
		);
	}
	
	@Bean
	@Override
	public LcReactiveDataAccessStrategy reactiveDataAccessStrategy(R2dbcConverter converter) {
		return new LcReactiveDataAccessStrategy(getDialect(getConnectionFactory()), (LcMappingR2dbcConverter) converter);
	}
	
	@Override
	public MappingR2dbcConverter r2dbcConverter(R2dbcMappingContext mappingContext, R2dbcCustomConversions r2dbcCustomConversions) {
		return new LcMappingR2dbcConverter(mappingContext, r2dbcCustomConversions);
	}
	
	@Bean
	@Override
	public R2dbcEntityTemplate r2dbcEntityTemplate(DatabaseClient databaseClient, ReactiveDataAccessStrategy dataAccessStrategy) {
		return new LcR2dbcEntityTemplate(getLcClient(databaseClient, dataAccessStrategy));
	}
	
	@Bean
	@Override
	public R2dbcMappingContext r2dbcMappingContext(Optional<NamingStrategy> namingStrategy, R2dbcCustomConversions r2dbcCustomConversions) {
		Assert.notNull(namingStrategy, "NamingStrategy must not be null!");

		LcR2dbcMappingContext mappingContext = new LcR2dbcMappingContext(namingStrategy.orElse(NamingStrategy.INSTANCE));
		mappingContext.setSimpleTypeHolder(r2dbcCustomConversions.getSimpleTypeHolder());

		return mappingContext;
	}

	@Override
	public ConnectionFactory connectionFactory() {
		return null;
	}
	
	@SuppressWarnings("java:S112")
	private ConnectionFactory getConnectionFactory() {
		Assert.notNull(context, "ApplicationContext is not yet initialized");

		String[] beanNamesForType = context.getBeanNamesForType(ConnectionFactory.class);

		for (String beanName : beanNamesForType) {

			if (beanName.equals(CONNECTION_FACTORY_BEAN_NAME)) {
				return context.getBean(CONNECTION_FACTORY_BEAN_NAME, ConnectionFactory.class);
			}
		}

		ConnectionFactory factory = connectionFactory();
		if (factory == null)
			throw new RuntimeException("No r2dbc connection factory defined");
		return factory;
	}
	
}
