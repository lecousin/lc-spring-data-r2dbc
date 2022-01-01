package net.lecousin.reactive.data.relational.query.operation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.query.SelectQuery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
				return Flux.merge(1, load, retrieve).then();
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
		List<Flux<?>> loads = new LinkedList<>();
		Map<RelationalPersistentEntity<?>, Map<Object, List<Consumer<Object>>>> map = toLoad;
		toLoad = new HashMap<>();
		for (Map.Entry<RelationalPersistentEntity<?>, Map<Object, List<Consumer<Object>>>> entity : map.entrySet()) {
			loads.add(
				op.lcClient.lazyLoad(entity.getValue().keySet(), entity.getKey())
				.doOnNext(loaded -> {
					for (Consumer<Object> consumer : entity.getValue().get(loaded))
						op.toCall(() -> consumer.accept(loaded));
				})
			);
		}
		return execute(loads);
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
		List<Flux<?>> loads = new LinkedList<>();
		for (Map.Entry<RelationalPersistentEntity<?>, Map<RelationalPersistentProperty, Map<Object, List<Consumer<Object>>>>> entity : map.entrySet()) {
			for (Map.Entry<RelationalPersistentProperty, Map<Object, List<Consumer<Object>>>> property : entity.getValue().entrySet()) {
				loads.add(
					op.lcClient.execute(
						SelectQuery.from(entity.getKey().getType(), "e")
						.where(net.lecousin.reactive.data.relational.query.criteria.Criteria.property("e", property.getKey().getName()).in(property.getValue().keySet())),
						null
					).map(e -> retrieved(e, op, property.getKey(), property.getValue()))
				);
			}
		}
		return execute(loads);
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
	
	private static Mono<Void> execute(List<Flux<?>> requests) {
		if (requests.isEmpty())
			return null;
		if (requests.size() == 1)
			return requests.get(0).then();
		if (requests.size() > 4)
			Flux.fromIterable(requests)
				.parallel()
				.runOn(Schedulers.parallel(), 4)
				.flatMap(s -> s)
				.then()
				;
		return Flux.merge(requests).then();
	}
	
}
