package net.lecousin.reactive.data.relational.postgres.test;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import net.lecousin.reactive.data.relational.postgres.PostgresConfiguration;

@Configuration
public class PostgresTestConfiguration extends PostgresConfiguration {

	private static EmbeddedPostgres epg;
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (epg != null)
					try {
						epg.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		});
		EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder();
		try {
			epg = builder.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	@Bean
	public PostgresqlConnectionFactory connectionFactory() {
		return new PostgresqlConnectionFactory(
			PostgresqlConnectionConfiguration.builder()
			.host("localhost")
			.port(epg.getPort())
			.username("postgres")
			.build()
		);
	}
	
}
