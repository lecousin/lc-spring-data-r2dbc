package net.lecousin.reactive.data.relational.query.operation;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;

/** Cache of entities to avoid considering 2 instances as different. */
class OperationEntityCache {

	private Map<RelationalPersistentEntity<?>, Map<Object, Object>> cache = new HashMap<>();

	/** Get it from cache or add it to cache. */
	@SuppressWarnings("java:S3824")
	public Object getInstance(Object instance, RelationalPersistentEntity<?> entity, PersistentPropertyAccessor<?> accessor) {
		if (!entity.hasIdProperty())
			return instance;
		
		Map<Object, Object> map = cache.computeIfAbsent(entity, e -> new HashMap<>());
		Object id = accessor.getProperty(entity.getRequiredIdProperty());
		if (id == null)
			return instance;
		Object known = map.get(id);
		if (known == null) {
			map.put(id, instance);
			return instance;
		}
		if (known == instance)
			return instance;
		return known;
	}
	
}
