package net.lecousin.reactive.data.relational;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yaml.snakeyaml.Yaml;

import net.lecousin.reactive.data.relational.enhance.Enhancer;

public class LcReactiveDataRelationalInitializer {

	private static final Log logger = LogFactory.getLog(LcReactiveDataRelationalInitializer.class);
	
	private static boolean initialized = false;
	
	private static class Config {
		private List<String> entities = new LinkedList<>();
	}
	
	private LcReactiveDataRelationalInitializer() {
		// no instance
	}
	
	public static void init() {
		if (initialized)
			return;
		initialized = true;
		logger.info("Initializing lc-reactive-data-relational");
		try {
			Enumeration<URL> urls = LcReactiveDataRelationalInitializer.class.getClassLoader().getResources("lc-reactive-data-relational.yaml");
			Config config = new Config();
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				logger.info("Loading configuration from " + url);
				loadConfiguration(url, config);
			}
			Enhancer.enhance(config.entities);
		} catch (Exception e) {
			logger.error("Error configuring lc-reactive-data-relational", e);
		}
	}
	
	private static void loadConfiguration(URL url, Config config) {
		try (InputStream input = url.openStream()) {
			Yaml yaml = new Yaml();
			Map<String, Object> root = yaml.load(input);
			if (root.containsKey("entities")) {
				configureEntities(config, "", root.get("entities"));
			}
		} catch (Exception e) {
			logger.error("Unable to read configuration file", e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static void configureEntities(Config config, String prefix, Object value) {
		if (value instanceof String)
			config.entities.add(prefix + (String)value);
		else if (value instanceof Collection)
			for (Object element : (Collection)value)
				configureEntities(config, prefix, element);
		else if (value instanceof Map)
			for (Map.Entry entry : ((Map<?,?>)value).entrySet())
				configureEntities(config, prefix + entry.getKey() + '.', entry.getValue());
		else
			logger.warn("Unexpected entity package value: " + value);
	}
	

}
