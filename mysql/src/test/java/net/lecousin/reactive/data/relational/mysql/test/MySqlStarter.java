package net.lecousin.reactive.data.relational.mysql.test;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.wix.mysql.EmbeddedMysql;
import com.wix.mysql.config.Charset;
import com.wix.mysql.config.MysqldConfig;

public class MySqlStarter {

	private com.wix.mysql.distribution.Version version;
	private static com.wix.mysql.distribution.Version launchVersion = null;
	private static EmbeddedMysql mysql = null;
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (mysql != null)
					mysql.stop();
			}
		});
	}
	
	public MySqlStarter(com.wix.mysql.distribution.Version version) {
		this.version = version;
	}
	
	@PostConstruct
	public void start() {
		if (version.equals(launchVersion))
			return;
		if (mysql != null) {
			mysql.stop();
			mysql = null;
		}
		MysqldConfig config = MysqldConfig.aMysqldConfig(version)
		    .withPort(3306)
		    .withUser("auser", "sa")
		    .withTimeout(2, TimeUnit.MINUTES)
		    .withCharset(Charset.UTF8)
		    .build();
		mysql = EmbeddedMysql.anEmbeddedMysql(config).addSchema("test").start();
		launchVersion = version;
	}

}
