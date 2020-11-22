package net.lecousin.reactive.data.relational.mysql.test.v5_7_27;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import net.lecousin.reactive.data.relational.mysql.MySqlConfiguration;
import net.lecousin.reactive.data.relational.mysql.test.MySqlStarter;

@Configuration
public class MySql_v5_7_27_TestConfiguration extends MySqlConfiguration {

	@Bean
	public MySqlStarter mysql() {
		return new MySqlStarter(com.wix.mysql.distribution.Version.v5_7_27);
	}
	
	@Override
	@Bean
	public ConnectionFactory connectionFactory() {
		return ConnectionFactories.get(
		    "r2dbcs:mysql://auser:sa@127.0.0.1:3306/test?sslMode=preferred"
		);
	}
	
}
