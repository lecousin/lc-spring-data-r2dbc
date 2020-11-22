package net.lecousin.reactive.data.relational.query.operation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;

import reactor.core.publisher.Mono;

class PropertyUpdater {
	
	private Map<RelationalPersistentEntity<?>, Map<RelationalPersistentProperty, Map<Object, Object>>> toUpdate = new HashMap<>();

	Mono<Void> doOperations(Operation op) {
		return doUpdate(op);
	}
	
	void update(RelationalPersistentEntity<?> entityType, RelationalPersistentProperty property, Object whereValueIs, Object newValue) {
		Map<RelationalPersistentProperty, Map<Object, Object>> map = toUpdate.computeIfAbsent(entityType, e -> new HashMap<>());
		Map<Object, Object> map2 = map.computeIfAbsent(property, p -> new HashMap<>());
		map2.put(whereValueIs, newValue);
	}


	private Mono<Void> doUpdate(Operation op) {
		List<Mono<Void>> calls = new LinkedList<>();
		Map<RelationalPersistentEntity<?>, Map<RelationalPersistentProperty, Map<Object, Object>>> todo = toUpdate;
		toUpdate = new HashMap<>();
		for (Map.Entry<RelationalPersistentEntity<?>, Map<RelationalPersistentProperty, Map<Object, Object>>> entity : todo.entrySet()) {
			for (Map.Entry<RelationalPersistentProperty, Map<Object, Object>> property : entity.getValue().entrySet()) {
				Map<Object, Set<Object>> reverseMap = new HashMap<>();
				for (Map.Entry<Object, Object> entry : property.getValue().entrySet()) {
					Set<Object> set = reverseMap.get(entry.getValue());
					if (set == null) {
						set = new HashSet<>();
						reverseMap.put(entry.getValue(), set);
					}
					set.add(entry.getKey());
				}
				for (Map.Entry<Object, Set<Object>> update : reverseMap.entrySet()) {
					Map<SqlIdentifier, Object> assignments = new HashMap<>();
					assignments.put(property.getKey().getColumnName(), update.getKey());
					calls.add(
						op.lcClient.getSpringClient().update()
						.table(entity.getKey().getTableName())
						.using(Update.from(assignments))
						.matching(Criteria.where(property.getKey().getName()).in(update.getValue()))
						.then()
					);
				}
			}
		}
		if (calls.isEmpty())
			return null;
		return Mono.when(calls);
	}

}
