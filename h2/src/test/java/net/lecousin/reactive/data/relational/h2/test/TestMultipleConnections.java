package net.lecousin.reactive.data.relational.h2.test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import net.lecousin.reactive.data.relational.test.multipleconnections.TestMultipleDatabaseConnections;

@ContextConfiguration(classes = TestMultipleConnections.H2DbUrls.class)
public class TestMultipleConnections extends TestMultipleDatabaseConnections {

	@Configuration
	public static class H2DbUrls implements DbUrls {
		@Override
		@Bean
		@Qualifier("db1Url")
		public String getFirstDbConnectionUrl() {
			return "r2dbc:pool:h2:mem:///db1;DB_CLOSE_DELAY=-1;";
		}
		
		@Override
		@Bean
		@Qualifier("db2Url")
		public String getSecondDbConnectionUrl() {
			return "r2dbc:pool:h2:mem:///db2;DB_CLOSE_DELAY=-1;";
		}
	}

}
