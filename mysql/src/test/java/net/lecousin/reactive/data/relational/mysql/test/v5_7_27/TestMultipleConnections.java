package net.lecousin.reactive.data.relational.mysql.test.v5_7_27;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import net.lecousin.reactive.data.relational.mysql.test.MySqlStarter;
import net.lecousin.reactive.data.relational.test.multipleconnections.TestMultipleDatabaseConnections;

@ContextConfiguration(classes = TestMultipleConnections.MySqlDbUrls.class)
public class TestMultipleConnections extends TestMultipleDatabaseConnections {

	@Configuration
	public static class MySqlDbUrls implements DbUrls {
		@Bean
		public MySqlStarter mysql() {
			return new MySqlStarter(com.wix.mysql.distribution.Version.v5_7_27);
		}
		
		@Override
		@Bean
		@Qualifier("db1Url")
		public String getFirstDbConnectionUrl() {
			return "r2dbcs:pool:mysql://auser:sa@127.0.0.1:" + MySqlStarter.getPort() + "/first?sslMode=preferred";
		}
		
		@Override
		@Bean
		@Qualifier("db2Url")
		public String getSecondDbConnectionUrl() {
			return "r2dbcs:pool:mysql://auser:sa@127.0.0.1:" + MySqlStarter.getPort() + "/second?sslMode=preferred";
		}
	}

}
