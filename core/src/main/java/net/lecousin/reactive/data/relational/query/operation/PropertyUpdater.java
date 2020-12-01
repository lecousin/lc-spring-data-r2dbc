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
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.Update;

import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.query.SqlQuery;
import net.lecousin.reactive.data.relational.query.operation.SaveProcessor.SaveRequest;
import net.lecousin.reactive.data.relational.sql.ColumnIncrement;
import reactor.core.publisher.Mono;

class PropertyUpdater extends AbstractProcessor<PropertyUpdater.Request> {
	
	private Map<RelationalPersistentEntity<?>, Map<RelationalPersistentProperty, Map<Object, Request>>> requests = new HashMap<>();
	
	static class Request extends AbstractProcessor.Request {
		RelationalPersistentProperty property;
		Object whereValueIs;
		Object newValue;
		
		Request(RelationalPersistentEntity<?> entityType, RelationalPersistentProperty property, Object whereValueIs, Object newValue) {
			super(entityType);
			this.property = property;
			this.whereValueIs = whereValueIs;
			this.newValue = newValue;
		}
	}
	
	Request update(RelationalPersistentEntity<?> entityType, RelationalPersistentProperty property, Object whereValueIs, Object newValue) {
		Map<RelationalPersistentProperty, Map<Object, Request>> map = requests.computeIfAbsent(entityType, e -> new HashMap<>());
		Map<Object, Request> map2 = map.computeIfAbsent(property, p -> new HashMap<>());
		return map2.computeIfAbsent(whereValueIs, e -> new Request(entityType, property, whereValueIs, newValue));
	}

	@Override
	protected Mono<Void> executeRequests(Operation op) {
		List<Mono<Void>> calls = new LinkedList<>();
		for (Map.Entry<RelationalPersistentEntity<?>, Map<RelationalPersistentProperty, Map<Object, Request>>> entity : requests.entrySet()) {
			RelationalPersistentProperty versionProperty = entity.getKey().getVersionProperty();
			for (Map.Entry<RelationalPersistentProperty, Map<Object, Request>> property : entity.getValue().entrySet()) {
				Map<Object, Set<Object>> reverseMap = new HashMap<>();
				List<Request> ready = new LinkedList<>();
				for (Map.Entry<Object, Request> entry : property.getValue().entrySet()) {
					if (!canExecuteRequest(entry.getValue()))
						continue;
					Set<Object> set = reverseMap.computeIfAbsent(entry.getValue().newValue, e -> new HashSet<>());
					set.add(entry.getKey());
					ready.add(entry.getValue());
				}
				if (reverseMap.isEmpty())
					continue;
				Table table = Table.create(entity.getKey().getTableName());
				for (Map.Entry<Object, Set<Object>> update : reverseMap.entrySet()) {
					SqlQuery<Update> query = new SqlQuery<>(op.lcClient);
					List<Expression> values = new ArrayList<>(update.getValue().size());
					for (Object value : update.getValue())
						values.add(query.marker(value));
					List<AssignValue> assignments = new LinkedList<>();
					assignments.add(AssignValue.create(Column.create(property.getKey().getColumnName(), table), update.getKey() != null ? query.marker(update.getKey()) : SQL.nullLiteral()));
					if (versionProperty != null)
						assignments.add(AssignValue.create(Column.create(versionProperty.getColumnName(), table), SQL.literalOf(new ColumnIncrement(Column.create(versionProperty.getColumnName(), table), op.lcClient))));
					Condition where = Conditions.in(Column.create(property.getKey().getColumnName(), table), values);
					for (SaveRequest save : op.save.getPendingRequests(entity.getKey(), s -> update.getValue().contains(ModelUtils.getPersistedDatabaseValue(s.state, property.getKey(), op.lcClient.getMappingContext())))) {
						if (save.state.isPersisted())
							where = where.and(ModelUtils.getConditionOnId(query, save.entityType, save.accessor, op.lcClient.getMappingContext()).not());
						save.accessor.setProperty(property.getKey(), update.getKey());
					}
					query.setQuery(
						Update.builder().table(table)
						.set(assignments)
						.where(where)
						.build()
					);
					calls.add(query.execute().then().doOnSuccess(v -> ready.forEach(r -> r.executed = true)));
				}
			}
		}
		if (calls.isEmpty())
			return null;
		return Mono.when(calls);
	}

}
