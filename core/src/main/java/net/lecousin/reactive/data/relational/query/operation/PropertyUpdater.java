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
import java.util.Set;

import org.springframework.data.relational.core.sql.AssignValue;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.Update;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;
import net.lecousin.reactive.data.relational.query.SqlQuery;
import net.lecousin.reactive.data.relational.query.operation.SaveProcessor.SaveRequest;
import net.lecousin.reactive.data.relational.sql.ColumnIncrement;
import reactor.core.publisher.Mono;

/**
 * Process update of specific rows in a global save operation.
 * 
 * @author Guillaume Le Cousin
 *
 */
class PropertyUpdater extends AbstractProcessor<PropertyUpdater.Request> {
	
	private Map<PropertyMetadata, Map<Object, Request>> requests = new HashMap<>();
	
	static class Request extends AbstractProcessor.Request {
		PropertyMetadata property;
		Object whereValueIs;
		Object newValue;
		
		Request(PropertyMetadata property, Object whereValueIs, Object newValue) {
			this.property = property;
			this.whereValueIs = whereValueIs;
			this.newValue = newValue;
		}
	}
	
	Request update(PropertyMetadata property, Object whereValueIs, Object newValue) {
		Map<Object, Request> map = requests.computeIfAbsent(property, p -> new HashMap<>());
		return map.computeIfAbsent(whereValueIs, w -> new Request(property, whereValueIs, newValue));
	}

	@Override
	@SuppressWarnings("java:S3776")
	protected Mono<Void> executeRequests(Operation op) {
		List<Mono<Void>> calls = new LinkedList<>();
		for (Map.Entry<PropertyMetadata, Map<Object, Request>> property : requests.entrySet()) {
			PropertyMetadata versionProperty = property.getKey().getEntity().getVersionProperty();
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
			executeUpdates(op, reverseMap, property.getKey(), versionProperty, ready, calls);
		}
		return Operation.executeParallel(calls);
	}
	
	private static void executeUpdates(
		Operation op, Map<Object, Set<Object>> reverseMap,
		PropertyMetadata property, @Nullable PropertyMetadata versionProperty,
		List<Request> ready, List<Mono<Void>> calls
	) {
		Table table = Table.create(property.getEntity().getTableName());
		for (Map.Entry<Object, Set<Object>> update : reverseMap.entrySet()) {
			SqlQuery<Update> query = new SqlQuery<>(op.lcClient);
			List<Expression> values = new ArrayList<>(update.getValue().size());
			for (Object value : update.getValue())
				values.add(query.marker(value));
			List<AssignValue> assignments = new LinkedList<>();
			assignments.add(AssignValue.create(Column.create(property.getColumnName(), table), update.getKey() != null ? query.marker(update.getKey()) : SQL.nullLiteral()));
			if (versionProperty != null)
				assignments.add(AssignValue.create(Column.create(versionProperty.getColumnName(), table), SQL.literalOf(new ColumnIncrement(Column.create(versionProperty.getColumnName(), table), op.lcClient))));
			Condition where = Conditions.in(Column.create(property.getColumnName(), table), values);
			for (SaveRequest save : op.save.getPendingRequests(property.getEntity(), s -> update.getValue().contains(ModelUtils.getPersistedDatabaseValue(s.entity.getState(), property)))) {
				where = where.and(save.entity.getConditionOnId(query).not());
				save.entity.setValue(property, update.getKey());
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
