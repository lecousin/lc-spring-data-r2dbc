/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lecousin.reactive.data.relational.model;

import java.util.HashMap;
import java.util.Map;

import net.lecousin.reactive.data.relational.model.metadata.EntityInstance;

/**
 * Cache of entity instances, to use the same instance when loading the same primary key in a database query.
 * 
 * @author Guillaume Le Cousin
 *
 */
public class EntityCache {

	private Map<Class<?>, Map<Object, EntityInstance<?>>> cache = new HashMap<>();

	@SuppressWarnings("unchecked")
	public <T> EntityInstance<T> getInstanceById(Class<T> type, Object id) {
		Map<Object, EntityInstance<?>> instances = cache.get(type);
		if (instances == null)
			return null;
		return (EntityInstance<T>) instances.get(id);
	}
	
	public <T> void setInstanceById(Object id, EntityInstance<T> instance) {
		Map<Object, EntityInstance<?>> instances = cache.computeIfAbsent(instance.getType(), t -> new HashMap<>());
		instances.put(id, instance);
	}
	
	/** Get it from cache or add it to cache. */
	@SuppressWarnings("java:S3824")
	public <T> EntityInstance<T> getOrSetInstance(EntityInstance<T> instance) {
		if (!instance.getState().isPersisted()) {
			return instance; // if not persisted, we cannot use id, only instance
		}
		Map<Object, EntityInstance<?>> map = cache.computeIfAbsent(instance.getType(), e -> new HashMap<>());
		Object id = instance.getId();
		@SuppressWarnings("unchecked")
		EntityInstance<T> known = (EntityInstance<T>) map.get(id);
		if (known == null) {
			map.put(id, instance);
			return instance;
		}
		if (known == instance || known.getEntity() == instance.getEntity())
			return instance;
		return known;
	}
	
	@SuppressWarnings("unchecked")
	public <T> EntityInstance<T> getInstance(T entity) {
		for (EntityInstance<?> instance : cache.get(entity.getClass()).values())
			if (instance.getEntity() == entity)
				return (EntityInstance<T>) instance;
		return null;
	}
	
}
