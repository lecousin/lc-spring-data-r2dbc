package myapp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import net.lecousin.reactive.data.relational.h2.H2Configuration;

@Configuration
public class H2TestConfiguration extends H2Configuration {

	@Override
	@Bean
	public H2ConnectionFactory connectionFactory() {
		return new H2ConnectionFactory(
			H2ConnectionConfiguration.builder()
			.url("mem:testdb;DB_CLOSE_DELAY=-1;")
			.username("sa")
			.build()
		);
	}
	
}
