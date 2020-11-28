package net.lecousin.reactive.data.relational.query.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.AssignValue;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.Update;

import net.lecousin.reactive.data.relational.query.SqlQuery;
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
				Table table = Table.create(entity.getKey().getTableName());
				for (Map.Entry<Object, Set<Object>> update : reverseMap.entrySet()) {
					SqlQuery<Update> query = new SqlQuery<>(op.lcClient);
					List<Expression> values = new ArrayList<>(update.getValue().size());
					for (Object value : update.getValue())
						values.add(query.marker(value));
					query.setQuery(
						Update.builder().table(table)
						.set(AssignValue.create(Column.create(property.getKey().getColumnName(), table), update.getKey() != null ? query.marker(update.getKey()) : SQL.nullLiteral()))
						.where(Conditions.in(Column.create(property.getKey().getColumnName(), table), values))
						.build()
					);
					calls.add(query.execute().then());
				}
			}
		}
		if (calls.isEmpty())
			return null;
		return Mono.when(calls);
	}

}
