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
import net.lecousin.reactive.data.relational.model.LcEntityTypeInfo;
import net.lecousin.reactive.data.relational.model.LcEntityTypeInfo.JoinTableInfo;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

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
	
	public static <T> SelectQuery<T> from(Class<T> type, String alias) {
		return new SelectQuery<>(type, alias);
	}

	public SelectQuery<T> join(String entityName, String propertyName, String alias) {
		TableReference source = tableAliases.get(entityName);
		if (source == null)
			throw new IllegalArgumentException("entity <" + entityName + "> does not exist in current select query");
		TableReference table = new TableReference(source, propertyName, null, alias);
		joins.add(table);
		tableAliases.put(table.alias, table);
		return this;
	}
	
	public SelectQuery<T> where(Criteria criteria) {
		if (where == null)
			where = criteria;
		else
			where = where.and(criteria);
		return this;
	}
	
	public SelectQuery<T> limit(long start, long nb) {
		this.offset = start;
		this.limit = nb;
		return this;
	}
	
	public SelectQuery<T> orderBy(String entityName, String propertyName, boolean ascending) {
		this.orderBy.add(Tuples.of(entityName, propertyName, Boolean.valueOf(ascending)));
		return this;
	}
	
	public Flux<T> execute(LcReactiveDataRelationalClient client) {
		return client.execute(this, null);
	}

	public Flux<T> execute(LcReactiveDataRelationalClient client, LcEntityReader reader) {
		return client.execute(this, reader);
	}
	
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
			RelationalPersistentProperty property = joinSourceEntity.getPersistentProperty(join.propertyName);
			if (property != null) {
				join.targetType = property.getActualType();
				continue;
			}
			LcEntityTypeInfo sourceInfo = LcEntityTypeInfo.get(join.source.targetType);
			Field f = sourceInfo.getForeignTableFieldForProperty(join.propertyName);
			if (f != null) {
				if (ModelUtils.isCollection(f))
					join.targetType = ModelUtils.getCollectionType(f);
				else
					join.targetType = f.getType();
				continue;
			}
			JoinTableInfo joinInfo = sourceInfo.getJoinTable(join.propertyName);
			if (joinInfo != null) {
				TableReference newJoin = new TableReference(join.source, joinInfo.getJoinForeignTable().getField().getName(), ModelUtils.getCollectionType(joinInfo.getJoinForeignTable().getField()), join.source.alias + "__join__" + join.alias);
				join.source = newJoin;
				join.propertyName = joinInfo.getJoinTargetFieldName();
				join.targetType = ModelUtils.getCollectionType(joinInfo.getField());
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
