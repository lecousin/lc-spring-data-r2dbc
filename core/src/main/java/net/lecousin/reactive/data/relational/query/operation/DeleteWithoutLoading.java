package net.lecousin.reactive.data.relational.query.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Delete;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.query.SqlQuery;
import reactor.core.publisher.Mono;

class DeleteWithoutLoading extends AbstractProcessor<DeleteWithoutLoading.Request> {

	/** Leaf entities that can be deleted without the need to load them. */
	private Map<RelationalPersistentEntity<?>, List<Request>> requests = new HashMap<>();

	static class Request extends AbstractProcessor.Request {
		RelationalPersistentProperty whereProperty;
		Object whereValue;
		
		Request(RelationalPersistentEntity<?> entityType, RelationalPersistentProperty whereProperty, Object whereValue) {
			super(entityType);
			this.whereProperty = whereProperty;
			this.whereValue = whereValue;
		}
	}
	
	Request addRequest(RelationalPersistentEntity<?> entity, RelationalPersistentProperty whereProperty, Object whereValue) {
		List<Request> list = requests.computeIfAbsent(entity, e -> new LinkedList<>());
		for (Request r : list)
			if (r.whereProperty.equals(whereProperty) && Objects.equals(r.whereValue, whereValue))
				return r;
		Request r = new Request(entity, whereProperty, whereValue);
		list.add(r);
		return r;
	}

	@Override
	@SuppressWarnings("java:S4449") // condition cannot be null because ready is not empty
	protected Mono<Void> executeRequests(Operation op) {
		List<Mono<Void>> calls = new LinkedList<>();
		for (Map.Entry<RelationalPersistentEntity<?>, List<Request>> entity : requests.entrySet()) {
			List<Request> ready = new LinkedList<>();
			for (Request r : entity.getValue())
				if (canExecuteRequest(r))
					ready.add(r);
			if (ready.isEmpty())
				continue;
			SqlQuery<Delete> query = new SqlQuery<>(op.lcClient);
			Table table = Table.create(entity.getKey().getTableName());
			Condition condition = createCondition(entity.getKey(), table, ready, query);
			query.setQuery(Delete.builder().from(table).where(condition).build());
			calls.add(query.execute().then().doOnSuccess(v -> ready.forEach(r -> r.executed = true)));
		}
		return Operation.executeParallel(calls);
	}
	
	private static Condition createCondition(RelationalPersistentEntity<?> entityType, Table table, List<Request> ready, SqlQuery<Delete> query) {
		Map<SqlIdentifier, Set<Object>> valuesByColumn = new HashMap<>();
		for (Request r : ready) {
			SqlIdentifier col = r.whereProperty.getColumnName();
			valuesByColumn.computeIfAbsent(col, c -> new HashSet<>()).add(r.whereValue);
		}

		Condition condition = null;
		for (Map.Entry<SqlIdentifier, Set<Object>> e : valuesByColumn.entrySet()) {
			Column col = Column.create(e.getKey(), table);
			List<Expression> list = new ArrayList<>(e.getValue().size());
			for (Object value : e.getValue())
				list.add(query.marker(value));
			Condition c = Conditions.in(col, list);
			condition = condition != null ? condition.or(c) : c;
		}

		if (LcReactiveDataRelationalClient.logger.isDebugEnabled())
			LcReactiveDataRelationalClient.logger.debug("Delete " + entityType.getType().getName() + " where " + condition);
		return condition;
	}

}
