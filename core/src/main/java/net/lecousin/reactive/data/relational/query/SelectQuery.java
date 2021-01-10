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
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
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
	List<Tuple2<String, Boolean>> orderBy = new LinkedList<>();
	
	private SelectQuery(Class<T> type, String alias) {
		from = new TableReference(null, null, type, alias);
		tableAliases.put(alias, from);
	}
	
	public static <T> SelectQuery<T> from(Class<T> type, String alias) {
		return new SelectQuery<>(type, alias);
	}

	public SelectQuery<T> join(String entityName, String propertyName, String alias) {
		TableReference source = tableAliases.get(entityName);
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
	
	public SelectQuery<T> orderBy(String rootPropertyName, boolean ascending) {
		this.orderBy.add(Tuples.of(rootPropertyName, Boolean.valueOf(ascending)));
		return this;
	}
	
	public Flux<T> execute(LcReactiveDataRelationalClient client) {
		return client.execute(this, null);
	}

	public Flux<T> execute(LcReactiveDataRelationalClient client, LcEntityReader reader) {
		return client.execute(this, reader);
	}
	
	
	void setJoinsTargetType(LcMappingR2dbcConverter mapper) {
		for (TableReference join : joins) {
			if (join.targetType == null) {
				RelationalPersistentEntity<?> joinSourceEntity = mapper.getMappingContext().getRequiredPersistentEntity(join.source.targetType);
				RelationalPersistentProperty property = joinSourceEntity.getPersistentProperty(join.propertyName);
				if (property != null) {
					join.targetType = property.getActualType();
				} else {
					Field f = ModelUtils.getRequiredForeignTableFieldForProperty(join.source.targetType, join.propertyName);
					if (ModelUtils.isCollection(f))
						join.targetType = ModelUtils.getCollectionType(f);
					else
						join.targetType = f.getType();
				}
			}
		}
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("Select from ").append(from.targetType.getSimpleName()).append(" as ").append(from.alias);
		for (TableReference join : joins) {
			s.append(" join ");
			if (join.targetType != null)
				s.append(join.targetType.getSimpleName()).append(" as ");
			s.append(join.alias);
		}
		if (where != null)
			s.append(" where ").append(where.toString());
		if (limit > 0)
			s.append(" limit ").append(offset).append(',').append(limit);
		if (!orderBy.isEmpty()) {
			s.append(" order by ");
			for (Tuple2<String, Boolean> o : orderBy)
				s.append(o.getT1()).append(o.getT2().booleanValue() ? " ASC" : " DESC");
		}
		return s.toString();
	}
	
}
