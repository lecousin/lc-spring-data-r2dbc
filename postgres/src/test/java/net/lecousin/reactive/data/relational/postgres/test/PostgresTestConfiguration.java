package net.lecousin.reactive.data.relational.postgres.test;

import java.io.IOException;
import java.sql.Connection;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import net.lecousin.reactive.data.relational.postgres.PostgresConfiguration;

@Configuration
public class PostgresTestConfiguration extends PostgresConfiguration {

	public static EmbeddedPostgres epg;
	private static Throwable error = null;
	
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
		EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder().setPGStartupWait(Duration.ofSeconds(30));
		try {
			epg = builder.start();
			Connection conn = epg.getPostgresDatabase().getConnection();
			conn.prepareCall("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"").execute();
			conn.prepareCall("CREATE DATABASE first").execute();
			conn.prepareCall("CREATE DATABASE second").execute();
			conn.close();
		} catch (Throwable e) {
			error = e;
		}
	}
	
	@Override
	@Bean
	public ConnectionFactory connectionFactory() {
		if (error != null)
			throw new RuntimeException("Postgres server not started", error);
		return ConnectionFactories.get("r2dbc:pool:postgresql://postgres@localhost:" + epg.getPort());
	}
	
}
