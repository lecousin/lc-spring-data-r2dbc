package net.lecousin.reactive.data.relational.configuration;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.r2dbc.spi.ConnectionFactory;
import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.dialect.SchemaGenerationDialect;
import net.lecousin.reactive.data.relational.mapping.LcMappingR2dbcConverter;
import net.lecousin.reactive.data.relational.mapping.LcReactiveDataAccessStrategy;

/**
 * Configure R2DBC spring data extended by lc-reactive-spring-data-relational.
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
	
	@Bean
	public LcReactiveDataRelationalClient lcClient() {
		return new LcReactiveDataRelationalClient();
	}
	
	public abstract SchemaGenerationDialect lcSchemaGenerationDialect(LcReactiveDataAccessStrategy dataAccess);
	
	@Bean
	@Override
	public ReactiveDataAccessStrategy reactiveDataAccessStrategy(R2dbcMappingContext mappingContext, R2dbcCustomConversions r2dbcCustomConversions) {

		Assert.notNull(mappingContext, "MappingContext must not be null!");

		LcMappingR2dbcConverter converter = new LcMappingR2dbcConverter(mappingContext, r2dbcCustomConversions, getClient());

		return new LcReactiveDataAccessStrategy(getDialect(getConnectionFactory()), converter);
	}
	
	private ConnectionFactory getConnectionFactory() {
		Assert.notNull(context, "ApplicationContext is not yet initialized");

		String[] beanNamesForType = context.getBeanNamesForType(ConnectionFactory.class);

		for (String beanName : beanNamesForType) {

			if (beanName.equals(CONNECTION_FACTORY_BEAN_NAME)) {
				return context.getBean(CONNECTION_FACTORY_BEAN_NAME, ConnectionFactory.class);
			}
		}

		return connectionFactory();
	}
	
	private LcReactiveDataRelationalClient getClient() {
		try {
			return context.getBean(LcReactiveDataRelationalClient.class);
		} catch (NoSuchBeanDefinitionException e) {
			return lcClient();
		}
	}
	
}
