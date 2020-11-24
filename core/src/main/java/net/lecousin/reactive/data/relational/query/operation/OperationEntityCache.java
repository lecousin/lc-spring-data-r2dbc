package net.lecousin.reactive.data.relational.query.operation;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.ModelUtils;

/** Cache of entities to avoid considering 2 instances as different. */
class OperationEntityCache {

	private Map<RelationalPersistentEntity<?>, Map<Object, Object>> cache = new HashMap<>();

	/** Get it from cache or add it to cache. */
	@SuppressWarnings("java:S3824")
	public Object getInstance(EntityState state, RelationalPersistentEntity<?> entity, PersistentPropertyAccessor<?> accessor, MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext) {
		if (!state.isPersisted())
			return accessor.getBean(); // if not persisted, we cannot use id, only instance
		Map<Object, Object> map = cache.computeIfAbsent(entity, e -> new HashMap<>());
		Object id = ModelUtils.getId(entity, accessor, mappingContext);
		if (id == null)
			return accessor.getBean();
		Object known = map.get(id);
		if (known == null) {
			map.put(id, accessor.getBean());
			return accessor.getBean();
		}
		if (known == accessor.getBean())
			return accessor.getBean();
		return known;
	}
	
}
