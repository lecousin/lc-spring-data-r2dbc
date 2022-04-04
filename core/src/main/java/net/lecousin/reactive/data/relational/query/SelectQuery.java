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
package net.lecousin.reactive.data.relational.query;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.mapping.LcEntityReader;
import net.lecousin.reactive.data.relational.mapping.LcMappingR2dbcConverter;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.model.metadata.EntityStaticMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyStaticMetadata;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

/**
 * Specifies a SELECT operation, possibly with joins, where clause, limit and order by clause.
 * 
 * @author Guillaume Le Cousin
 * 
 * @param <T> type of entity this select will return
 */
public class SelectQuery<T> {
	
	static class TableReference {
		
		TableReference source;
		String propertyName;
		Class<?> targetType;
		String alias;
		
		private TableReference(TableReference source, String propertyName, Class<?> targetType, String alias) {
			this.source = source;
			this.propertyName = propertyName;
			this.targetType = targetType;
			this.alias = alias;
		}

	}

	TableReference from;
	List<TableReference> joins = new LinkedList<>();
	Map<String, TableReference> tableAliases = new HashMap<>();
	Criteria where = null;
	long offset = 0;
	long limit = -1;
	List<Tuple3<String, String, Boolean>> orderBy = new LinkedList<>();
	
	private SelectQuery(Class<T> type, String alias) {
		from = new TableReference(null, null, type, alias);
		tableAliases.put(alias, from);
	}
	
	/** Create a SELECT query from the table of the given entity type, using the given alias. */
	public static <T> SelectQuery<T> from(Class<T> type, String alias) {
		return new SelectQuery<>(type, alias);
	}

	/** Create a join, using the link entityName.propertyName, and using the given alias as the joined entity name. */
	public SelectQuery<T> join(String entityName, String propertyName, String alias) {
		TableReference source = tableAliases.get(entityName);
		if (source == null)
			throw new IllegalArgumentException("entity <" + entityName + "> does not exist in current select query");
		TableReference table = new TableReference(source, propertyName, null, alias);
		joins.add(table);
		tableAliases.put(table.alias, table);
		return this;
	}
	
	/** Set the given criteria in the where clause. If criteria already exist, a AND is created between the existing criteria and the new criteria. */
	public SelectQuery<T> where(Criteria criteria) {
		if (where == null)
			where = criteria;
		else
			where = where.and(criteria);
		return this;
	}
	
	/** Apply a limit and offset to the select query. */
	public SelectQuery<T> limit(long start, long count) {
		this.offset = start;
		this.limit = count;
		return this;
	}
	
	/** Add an ORDER BY clause to this select query. */
	public SelectQuery<T> orderBy(String entityName, String propertyName, boolean ascending) {
		this.orderBy.add(Tuples.of(entityName, propertyName, Boolean.valueOf(ascending)));
		return this;
	}
	
	/** Execute the query using the given database client. */
	public Flux<T> execute(LcReactiveDataRelationalClient client) {
		return client.execute(this, null);
	}

	/** Execute the query using the given database client and the given reader. */
	public Flux<T> execute(LcReactiveDataRelationalClient client, LcEntityReader reader) {
		return client.execute(this, reader);
	}
	
	/** Execute a COUNT request, using the joins and where clause of this select query. Any join which are not useful (not used for conditions) are automatically ignored. */
	public Mono<Long> executeCount(LcReactiveDataRelationalClient client) {
		return client.executeCount(this);
	}
	
	
	@SuppressWarnings("java:S135")
	void setJoinsTargetType(LcMappingR2dbcConverter mapper) {
		for (int i = 0; i < joins.size(); ++i) {
			TableReference join = joins.get(i);
			if (join.targetType != null)
				continue;
			RelationalPersistentEntity<?> joinSourceEntity = mapper.getMappingContext().getRequiredPersistentEntity(join.source.targetType);
			RelationalPersistentProperty joinProperty = joinSourceEntity.getPersistentProperty(join.propertyName);
			if (joinProperty != null) {
				join.targetType = joinProperty.getActualType();
				continue;
			}
			EntityStaticMetadata sourceInfo = EntityStaticMetadata.get(join.source.targetType);
			PropertyStaticMetadata property = sourceInfo.getRequiredProperty(join.propertyName);
			if (property.isForeignTable()) {
				join.targetType = property.getTypeOrCollectionElementType();
				continue;
			}
			if (property.isJoinTable()) {
				Field joinTableFDoreignTableField = property.getJoinTableForeignTable().getField();
				TableReference newJoin = new TableReference(join.source, joinTableFDoreignTableField.getName(), ModelUtils.getCollectionType(joinTableFDoreignTableField), join.source.alias + "__join__" + join.alias);
				join.source = newJoin;
				join.propertyName = property.getJoinTableTargetFieldName();
				join.targetType = property.getCollectionElementType();
				tableAliases.put(newJoin.alias, newJoin);
				joins.add(i, newJoin);
				continue;
			}
			throw new ModelAccessException("Cannot join on property " + join.source.targetType.getSimpleName() + '#' + join.propertyName);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("SELECT FROM ").append(from.targetType.getSimpleName()).append(" AS ").append(from.alias);
		for (TableReference join : joins) {
			s.append(" JOIN ");
			if (join.targetType != null)
				s.append(join.targetType.getSimpleName()).append(" AS ");
			s.append(join.alias);
		}
		if (where != null)
			s.append(" WHERE ").append(where.toString());
		if (limit > 0)
			s.append(" LIMIT ").append(offset).append(',').append(limit);
		if (!orderBy.isEmpty()) {
			s.append(" ORDER BY ");
			for (Tuple3<String, String, Boolean> o : orderBy)
				s.append(o.getT1()).append('.').append(o.getT2()).append(o.getT3().booleanValue() ? " ASC" : " DESC");
		}
		return s.toString();
	}
	
}
