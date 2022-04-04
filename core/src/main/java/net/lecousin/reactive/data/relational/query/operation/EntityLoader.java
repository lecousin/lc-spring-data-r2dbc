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
package net.lecousin.reactive.data.relational.query.operation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.data.mapping.MappingException;

import net.lecousin.reactive.data.relational.model.metadata.EntityInstance;
import net.lecousin.reactive.data.relational.model.metadata.EntityMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;
import net.lecousin.reactive.data.relational.query.SelectQuery;
import reactor.core.publisher.Mono;

/**
 * Load entities when needed to analyze cascade operations.
 * 
 * @author Guillaume Le Cousin
 *
 */
class EntityLoader {

	/** Entities to load to retrieve linked ids. Consumers may then add thinks to be deleted and add dependencies. */
	private Map<EntityMetadata, Map<EntityInstance<?>, List<Consumer<EntityInstance<?>>>>> toLoad = new HashMap<>();
	
	/** Entities to retrieve (select), by table, where property in values. Once retrieved, the consumers are called. */
	private Map<EntityMetadata, Map<PropertyMetadata, Map<Object, List<Consumer<EntityInstance<?>>>>>> toRetrieve = new HashMap<>();
	
	Mono<Void> doOperations(Operation op) {
		List<Mono<Void>> loads = new LinkedList<>();
		doLoad(op, loads);
		doRetrieve(op, loads);
		return Operation.executeParallel(loads);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	<T> void load(EntityInstance<T> instance, Consumer<EntityInstance<T>> onLoaded) {
		Map<EntityInstance<?>, List<Consumer<EntityInstance<?>>>> map = toLoad.computeIfAbsent(instance.getMetadata(), e -> new HashMap<>());
		List<Consumer<EntityInstance<?>>> consumers = map.computeIfAbsent(instance, v -> new LinkedList<>());
		consumers.add((Consumer)onLoaded);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void doLoad(Operation op, List<Mono<Void>> loads) {
		Map<EntityMetadata, Map<EntityInstance<?>, List<Consumer<EntityInstance<?>>>>> map = toLoad;
		toLoad = new HashMap<>();
		for (Map.Entry<EntityMetadata, Map<EntityInstance<?>, List<Consumer<EntityInstance<?>>>>> entity : map.entrySet()) {
			loads.add(
				op.lcClient.lazyLoadInstances((Set) entity.getValue().keySet())
				.doOnNext(loaded -> {
					for (Consumer consumer : entity.getValue().get(loaded))
						op.toCall(() -> consumer.accept(loaded));
				}).then()
			);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	<T> void retrieve(PropertyMetadata property, Object propertyValue, Consumer<EntityInstance<T>> consumer) {
		EntityMetadata type = property.getEntity();
		Map<PropertyMetadata, Map<Object, List<Consumer<EntityInstance<?>>>>> map = toRetrieve.computeIfAbsent(type, e -> new HashMap<>());
		Map<Object, List<Consumer<EntityInstance<?>>>> byValue = map.computeIfAbsent(property, p -> new HashMap<>());
		List<Consumer<EntityInstance<?>>> list = byValue.computeIfAbsent(propertyValue, v -> new LinkedList<>());
		list.add((Consumer) consumer);
	}
	
	private void doRetrieve(Operation op, List<Mono<Void>> loads) {
		Map<EntityMetadata, Map<PropertyMetadata, Map<Object, List<Consumer<EntityInstance<?>>>>>> map = toRetrieve;
		toRetrieve = new HashMap<>();
		for (Map.Entry<EntityMetadata, Map<PropertyMetadata, Map<Object, List<Consumer<EntityInstance<?>>>>>> entity : map.entrySet()) {
			for (Map.Entry<PropertyMetadata, Map<Object, List<Consumer<EntityInstance<?>>>>> property : entity.getValue().entrySet()) {
				loads.add(
					op.lcClient.execute(
						SelectQuery.from(entity.getKey().getType(), "e")
						.where(net.lecousin.reactive.data.relational.query.criteria.Criteria.property("e", property.getKey().getName()).in(property.getValue().keySet())),
						null
					).map(e -> retrieved(e, op, property.getKey(), property.getValue())).then()
				);
			}
		}
	}
	
	private static <T> T retrieved(T entity, Operation op, PropertyMetadata property, Map<Object, List<Consumer<EntityInstance<?>>>> consumersMap) {
		try {
			EntityInstance<T> instance = op.lcClient.getInstance(entity);
			List<Consumer<EntityInstance<?>>> consumers = consumersMap.get(instance.getDatabaseValue(property));
			if (consumers != null)
				for (Consumer<EntityInstance<?>> c : consumers)
					op.toCall(() -> c.accept(instance));
			return entity;
		} catch (Exception e) {
			throw new MappingException("Error analyzing data from retrieved entity", e);
		}
	}
	
}
