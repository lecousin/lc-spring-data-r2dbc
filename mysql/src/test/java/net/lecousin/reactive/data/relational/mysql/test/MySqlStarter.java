package net.lecousin.reactive.data.relational.mysql.test;

import java.util.TimeZone;
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
		    .withPort(getPort())
		    .withUser("auser", "sa")
		    .withTimeout(2, TimeUnit.MINUTES)
		    .withCharset(Charset.aCharset("utf8", "utf8_bin"))
		    .withTimeZone(TimeZone.getDefault())
		    .build();
		mysql = EmbeddedMysql.anEmbeddedMysql(config).addSchema("test").addSchema("first").addSchema("second").start();
		launchVersion = version;
	}
	
	public static int getPort() {
		String s = System.getProperty("mysql.port");
		try {
			if (s != null)
				return Integer.valueOf(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
		s = System.getenv("mysql.port");
		try {
			if (s != null)
				return Integer.valueOf(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 3306;
	}

}
