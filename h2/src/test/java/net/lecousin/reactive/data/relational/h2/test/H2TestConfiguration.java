package net.lecousin.reactive.data.relational.h2.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import net.lecousin.reactive.data.relational.h2.H2Configuration;

@Configuration
public class H2TestConfiguration extends H2Configuration {

	@Override
	@Bean
	public ConnectionFactory connectionFactory() {
		return ConnectionFactories.get("r2dbc:pool:h2:mem:///sessions;DB_CLOSE_DELAY=-1;");
	}
	
}
