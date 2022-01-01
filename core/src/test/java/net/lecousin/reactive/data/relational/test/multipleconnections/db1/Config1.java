package net.lecousin.reactive.data.relational.test.multipleconnections.db1;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import net.lecousin.reactive.data.relational.configuration.LcR2dbcEntityOperationsBuilder;
import net.lecousin.reactive.data.relational.repository.LcR2dbcEntityTemplate;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;

@Configuration
@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class, basePackages = "net.lecousin.reactive.data.relational.test.multipleconnections.db1", entityOperationsRef = "db1Operations")
public class Config1 extends LcR2dbcEntityOperationsBuilder {

	@Bean
	@Qualifier("db1DatabaseConnectionFactory")
	public ConnectionFactory db1DatabaseConnectionFactory(@Qualifier("db1Url") String databaseUrl) {
		return ConnectionFactories.get(databaseUrl);
	}
	
	@Bean
	@Qualifier("db1Operations")
	public LcR2dbcEntityTemplate db1Operations(@Qualifier("db1DatabaseConnectionFactory") ConnectionFactory connectionFactory) {
		return buildEntityOperations(connectionFactory);
	}

}