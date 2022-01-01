package net.lecousin.reactive.data.relational.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.enhance.EntityState;

public class EntityCache {

	private Map<Class<?>, Map<Object, Object>> cache = new HashMap<>();

	@SuppressWarnings("unchecked")
	public <T> T getById(Class<T> type, Object id) {
		Map<Object, Object> instances = cache.get(type);
		if (instances == null)
			return null;
		return (T) instances.get(id);
	}
	
	public <T> void setById(Class<T> type, Object id, T instance) {
		Map<Object, Object> instances = cache.computeIfAbsent(type, t -> new HashMap<>());
		instances.put(id, instance);
	}
	
	/** Get it from cache or add it to cache. */
	@SuppressWarnings("java:S3824")
	public <T> T getOrSet(EntityState state, RelationalPersistentEntity<T> entity, PersistentPropertyAccessor<T> accessor, LcReactiveDataRelationalClient client) {
		if (!state.isPersisted())
			return accessor.getBean(); // if not persisted, we cannot use id, only instance
		Map<Object, Object> map = cache.computeIfAbsent(entity.getType(), e -> new HashMap<>());
		Object id = ModelUtils.getId(entity, accessor, client);
		@SuppressWarnings("unchecked")
		T known = (T) map.get(id);
		if (known == null) {
			map.put(id, accessor.getBean());
			return accessor.getBean();
		}
		if (known == accessor.getBean())
			return accessor.getBean();
		return known;
	}
	
}
