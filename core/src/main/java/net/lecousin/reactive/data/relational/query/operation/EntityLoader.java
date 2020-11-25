package net.lecousin.reactive.data.relational.query.operation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.query.SelectQuery;
import reactor.core.publisher.Mono;

class EntityLoader {

	/** Entities to load to retrieve linked ids. Consumers may then add thinks to be deleted and add dependencies. */
	private Map<RelationalPersistentEntity<?>, Map<Object, List<Consumer<Object>>>> toLoad = new HashMap<>();
	
	/** Entities to retrieve (select), by table, where property in values. Once retrieved, the consumers are called. */
	private Map<RelationalPersistentEntity<?>, Map<RelationalPersistentProperty, Map<Object, List<Consumer<Object>>>>> toRetrieve = new HashMap<>();
	
	Mono<Void> doOperations(Operation op) {
		Mono<Void> load = doLoad(op);
		Mono<Void> retrieve = doRetrieve(op);
		if (load != null) {
			if (retrieve != null)
				return Mono.when(load, retrieve);
			return load;
		}
		if (retrieve != null)
			return retrieve;
		return null;
	}
	
	@SuppressWarnings("unchecked")
	<T> void load(RelationalPersistentEntity<?> entity, T instance, Consumer<T> onLoaded) {
		Map<Object, List<Consumer<Object>>> map = toLoad.computeIfAbsent(entity, e -> new HashMap<>());
		List<Consumer<Object>> consumers = map.computeIfAbsent(instance, v -> new LinkedList<>());
		consumers.add((Consumer<Object>)onLoaded);
	}

	private Mono<Void> doLoad(Operation op) {
		// TODO do it in group instead of one by one
		List<Mono<?>> loads = new LinkedList<>();
		Map<RelationalPersistentEntity<?>, Map<Object, List<Consumer<Object>>>> map = toLoad;
		toLoad = new HashMap<>();
		for (Map.Entry<RelationalPersistentEntity<?>, Map<Object, List<Consumer<Object>>>> entity : map.entrySet()) {
			for (Map.Entry<Object, List<Consumer<Object>>> instance : entity.getValue().entrySet()) {
				loads.add(
					op.lcClient.lazyLoad(instance.getKey(), entity.getKey())
					.map(loaded -> {
						for (Consumer<Object> consumer : instance.getValue())
							op.toCall(() -> consumer.accept(loaded));
						return loaded;
					})
				);
			}
		}
		if (loads.isEmpty())
			return null;
		return Mono.when(loads);
	}
	
	void retrieve(RelationalPersistentEntity<?> entity, RelationalPersistentProperty property, Object propertyValue, Consumer<Object> consumer) {
		Map<RelationalPersistentProperty, Map<Object, List<Consumer<Object>>>> map = toRetrieve.computeIfAbsent(entity, e -> new HashMap<>());
		Map<Object, List<Consumer<Object>>> byValue = map.computeIfAbsent(property, p -> new HashMap<>());
		List<Consumer<Object>> list = byValue.computeIfAbsent(propertyValue, v -> new LinkedList<>());
		list.add(consumer);
	}
	
	private Mono<Void> doRetrieve(Operation op) {
		Map<RelationalPersistentEntity<?>, Map<RelationalPersistentProperty, Map<Object, List<Consumer<Object>>>>> map = toRetrieve;
		toRetrieve = new HashMap<>();
		List<Publisher<?>> loads = new LinkedList<>();
		for (Map.Entry<RelationalPersistentEntity<?>, Map<RelationalPersistentProperty, Map<Object, List<Consumer<Object>>>>> entity : map.entrySet()) {
			for (Map.Entry<RelationalPersistentProperty, Map<Object, List<Consumer<Object>>>> property : entity.getValue().entrySet()) {
				loads.add(
					op.lcClient.execute(
						SelectQuery.from(entity.getKey().getType(), "e")
						.where(net.lecousin.reactive.data.relational.query.criteria.Criteria.property("e", property.getKey().getName()).in(property.getValue().keySet()))
					).map(e -> retrieved(e, op, property.getKey(), property.getValue()))
				);
			}
		}
		if (loads.isEmpty())
			return null;
		return Mono.when(loads);
	}
	
	private static <T> T retrieved(T entity, Operation op, RelationalPersistentProperty property, Map<Object, List<Consumer<Object>>> consumersMap) {
		try {
			List<Consumer<Object>> consumers = consumersMap.get(ModelUtils.getDatabaseValue(entity, property, op.lcClient.getMappingContext()));
			if (consumers != null)
				for (Consumer<Object> c : consumers)
					op.toCall(() -> c.accept(entity));
			return entity;
		} catch (Exception e) {
			throw new MappingException("Error analyzing data from retrieved entity", e);
		}
	}
	
}
