package net.lecousin.reactive.data.relational.mapping;

import java.util.HashMap;
import java.util.Map;

public class ResultEntityCache {

	private Map<Class<?>, Map<Object, Object>> cache = new HashMap<>();

	public Object getCachedInstance(Class<?> type, Object id) {
		Map<Object, Object> instances = cache.get(type);
		if (instances == null)
			return null;
		return instances.get(id);
	}
	
	public void setCachedInstance(Class<?> type, Object id, Object instance) {
		Map<Object, Object> instances = cache.get(type);
		if (instances == null) {
			instances = new HashMap<>();
			cache.put(type, instances);
		}
		instances.put(id, instance);
	}
	
}
