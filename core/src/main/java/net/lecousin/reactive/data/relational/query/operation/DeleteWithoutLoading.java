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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Delete;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;
import net.lecousin.reactive.data.relational.query.SqlQuery;
import reactor.core.publisher.Mono;

/**
 * Delete rows using a where clause without needing to load the entities through a select before.
 * 
 * @author Guillaume Le Cousin
 *
 */
class DeleteWithoutLoading extends AbstractProcessor<DeleteWithoutLoading.Request> {

	/** Leaf entities that can be deleted without the need to load them. */
	private Map<PropertyMetadata, List<Request>> requests = new HashMap<>();

	static class Request extends AbstractProcessor.Request {
		PropertyMetadata whereProperty;
		Object whereValue;
		
		Request(PropertyMetadata whereProperty, Object whereValue) {
			this.whereProperty = whereProperty;
			this.whereValue = whereValue;
		}
	}
	
	Request addRequest(PropertyMetadata whereProperty, Object whereValue) {
		List<Request> list = requests.computeIfAbsent(whereProperty, p -> new LinkedList<>());
		for (Request r : list)
			if (r.whereProperty.equals(whereProperty) && Objects.equals(r.whereValue, whereValue))
				return r;
		Request r = new Request(whereProperty, whereValue);
		list.add(r);
		return r;
	}

	@Override
	@SuppressWarnings("java:S4449") // condition cannot be null because ready is not empty
	protected Mono<Void> executeRequests(Operation op) {
		List<Mono<Void>> calls = new LinkedList<>();
		for (Map.Entry<PropertyMetadata, List<Request>> property : requests.entrySet()) {
			List<Request> ready = new LinkedList<>();
			for (Request r : property.getValue())
				if (canExecuteRequest(r))
					ready.add(r);
			if (ready.isEmpty())
				continue;
			SqlQuery<Delete> query = new SqlQuery<>(op.lcClient);
			Table table = Table.create(property.getKey().getEntity().getTableName());
			Condition condition = createCondition(table, ready, query);
			query.setQuery(Delete.builder().from(table).where(condition).build());
			calls.add(query.execute().then().doOnSuccess(v -> ready.forEach(r -> r.executed = true)));
		}
		return Operation.executeParallel(calls);
	}
	
	private static Condition createCondition(Table table, List<Request> ready, SqlQuery<Delete> query) {
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
			LcReactiveDataRelationalClient.logger.debug("Delete " + table.getName() + " where " + condition);
		return condition;
	}

}
