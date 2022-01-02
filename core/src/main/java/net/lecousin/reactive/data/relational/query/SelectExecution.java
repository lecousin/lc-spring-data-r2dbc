package net.lecousin.reactive.data.relational.query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.OrderByField;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.SelectBuilder.BuildSelect;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectFromAndJoin;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectFromAndOrderBy;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectJoin;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectOrdered;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectWhere;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.annotations.CompositeId;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.mapping.LcEntityReader;
import net.lecousin.reactive.data.relational.model.LcEntityTypeInfo;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.model.PropertiesSource;
import net.lecousin.reactive.data.relational.model.PropertiesSourceMap;
import net.lecousin.reactive.data.relational.query.SelectQuery.TableReference;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.PropertyOperand;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.PropertyOperation;
import net.lecousin.reactive.data.relational.query.criteria.CriteriaSqlBuilder;
import net.lecousin.reactive.data.relational.query.criteria.CriteriaVisitor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

public class SelectExecution<T> {
	
	private static final Log logger = LogFactory.getLog(SelectExecution.class);
	
	private SelectQuery<T> query;
	private LcReactiveDataRelationalClient client;
	private LcEntityReader reader;
	
	public SelectExecution(SelectQuery<T> query, LcReactiveDataRelationalClient client, @Nullable LcEntityReader reader) {
		this.query = query;
		this.client = client;
		this.reader = reader != null ? reader : new LcEntityReader(null, client.getMapper());
	}
	
	public Flux<T> execute() {
		return Mono.fromCallable(this::needsPreSelectIds)
			.flatMapMany(needsPreSelect -> needsPreSelect.booleanValue() ? executeWithPreSelect() : executeWithoutPreSelect())
			.checkpoint(query.toString())
			;
	}
	
	public Mono<Long> executeCount() {
		query.setJoinsTargetType(client.getMapper());
		RelationalPersistentEntity<?> entity = client.getMappingContext().getRequiredPersistentEntity(query.from.targetType);
		SelectMapping mapping = buildSelectMapping();
		List<Expression> idColumns;
		if (entity.hasIdProperty())
			idColumns = Collections.singletonList(Column.create(entity.getIdColumn(), mapping.tableByAlias.get(query.from.alias)));
		else if (entity.isAnnotationPresent(CompositeId.class)) {
			String[] properties = entity.getRequiredAnnotation(CompositeId.class).properties();
			idColumns = new ArrayList<>(properties.length);
			for (String property : properties)
				idColumns.add(Column.create(entity.getRequiredPersistentProperty(property).getColumnName(), mapping.tableByAlias.get(query.from.alias)));
		} else
			throw new IllegalArgumentException("Cannot count distinct entities without an Id column or a CompoisteId");
		BuildSelect select = Select.builder()
			.select(client.getSchemaDialect().countDistinct(idColumns))
			.from(mapping.tableByAlias.get(query.from.alias));
		
		for (TableReference join : query.joins) {
			if (!needsTableForPreSelect(join, false))
				continue;
			select = join(select, join, mapping);
		}

		SqlQuery<Select> q = new SqlQuery<>(client);
		if (query.where != null) {
			select = ((SelectWhere)select).where(query.where.accept(new CriteriaSqlBuilder(mapping.entitiesByAlias, mapping.tableByAlias, q)));
		}
		
		q.setQuery(select.build());

		return q.execute().fetch().one().map(m -> (Long) m.values().iterator().next());
	}
	
	private boolean needsPreSelectIds() {
		// first step is to ensure we wave the target type for all joins
		query.setJoinsTargetType(client.getMapper());
		if (!hasJoinMany())
			return false;
		if (query.limit > 0)
			return true;
		return hasOrderByOnSubEntityOrOrderByWithConditionOnSubEntity() || hasConditionOnManyEntity();
	}
	
	private boolean hasJoinMany() {
		for (TableReference join : query.joins)
			if (isMany(join))
				return true;
		return false;
	}
	
	private boolean isMany(TableReference table) {
		if (table.source == null)
			return false;
		RelationalPersistentEntity<?> entity = client.getMappingContext().getRequiredPersistentEntity(table.source.targetType);
		try {
			Field field = entity.getType().getDeclaredField(table.propertyName);
			return ModelUtils.isCollection(field);
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean isManyFromRoot(TableReference table) {
		while (table.source != null) {
			if (isMany(table))
				return true;
			table = table.source;
		}
		return false;
	}
	
	private boolean hasOrderByOnSubEntityOrOrderByWithConditionOnSubEntity() {
		if (query.orderBy.isEmpty())
			return false;
		for (Tuple3<String, String, Boolean> order : query.orderBy) {
			TableReference table = query.tableAliases.get(order.getT1());
			if (table != query.from)
				return true;
		}
		return hasConditionOnSubEntity();
	}
	
	private boolean hasConditionOnSubEntity() {
		if (query.where == null)
			return false;
		Boolean found = query.where.accept(new CriteriaVisitor.SearchVisitor() {
			@Override
			public Boolean visit(PropertyOperation op) {
				TableReference table = query.tableAliases.get(op.getLeft().getEntityName());
				if (table != query.from)
					return Boolean.TRUE;
				if (op.getValue() instanceof PropertyOperand) {
					table = query.tableAliases.get(((PropertyOperand)op.getValue()).getEntityName());
					if (table != query.from)
						return Boolean.TRUE;
				}
				return Boolean.FALSE;
			}
		});
		return found.booleanValue();
	}
	
	private boolean hasConditionOnManyEntity() {
		if (query.where == null)
			return false;
		Boolean found = query.where.accept(new CriteriaVisitor.SearchVisitor() {
			@Override
			public Boolean visit(PropertyOperation op) {
				TableReference table = query.tableAliases.get(op.getLeft().getEntityName());
				if (isManyFromRoot(table))
					return Boolean.TRUE;
				if (op.getValue() instanceof PropertyOperand) {
					table = query.tableAliases.get(((PropertyOperand)op.getValue()).getEntityName());
					if (isManyFromRoot(table))
						return Boolean.TRUE;
				}
				return Boolean.FALSE;
			}
		});
		return found.booleanValue();
	}
	
	private static boolean isSourceFor(TableReference t1, TableReference t2) {
		while (t2 != null) {
			if (t1 == t2)
				return true;
			t2 = t2.source;
		}
		return false;
	}
	
	private boolean needsTableForPreSelect(TableReference table, boolean includeOrderBy) {
		if (includeOrderBy) {
			for (Tuple3<String, String, Boolean> order : query.orderBy) {
				TableReference t = query.tableAliases.get(order.getT1());
				if (isSourceFor(table, t))
					return true;
			}
		}
		if (query.where == null)
			return false;
		Boolean found = query.where.accept(new CriteriaVisitor.SearchVisitor() {
			@Override
			public Boolean visit(PropertyOperation op) {
				TableReference t = query.tableAliases.get(op.getLeft().getEntityName());
				if (isSourceFor(table, t))
					return Boolean.TRUE;
				if (op.getValue() instanceof PropertyOperand) {
					t = query.tableAliases.get(((PropertyOperand)op.getValue()).getEntityName());
					if (isSourceFor(table, t))
						return Boolean.TRUE;
				}
				return Boolean.FALSE;
			}
		});
		return found.booleanValue();
	}
	
	private Flux<T> executeWithPreSelect() {
		SelectMapping mapping = buildSelectMapping();
		return buildDistinctRootIdSql(mapping).execute().fetch().all()
			.map(row -> row.values().iterator().next())
			.buffer(100)
			.flatMap(ids -> {
				logger.debug("Pre-selected ids bunch: " + Objects.toString(ids));
				String idPropertyName = mapping.entitiesByAlias.get(query.from.alias).getIdProperty().getName();
				Flux<Map<String, Object>> fromDb = buildFinalSql(mapping, Criteria.property(query.from.alias, idPropertyName).in(ids), false, false).execute().fetch().all();
				RowHandler handler = new RowHandler(mapping);
				return Flux.create((Consumer<FluxSink<T>>)sink ->
					fromDb.doOnComplete(() -> handler.handleEnd(sink))
						.subscribe(row -> handler.handleRow(row, sink))
				)
				.collectList()
				.flatMapMany(list -> {
					List<T> orderedList = new ArrayList<>(list.size());
					List<T> rawList = new ArrayList<>(list);
					RelationalPersistentEntity<?> entityType = mapping.entitiesByAlias.get(query.from.alias);
					for (Object id : ids) {
						for (Iterator<T> it = rawList.iterator(); it.hasNext(); ) {
							T entity = it.next();
							Object entityId = ModelUtils.getRequiredId(entity, entityType, null);
							if (entityId.equals(id)) {
								it.remove();
								orderedList.add(entity);
								break;
							}
						}
					}
					return Flux.fromIterable(orderedList);
				});
			});
	}
	
	private Flux<T> executeWithoutPreSelect() {
		SelectMapping mapping = buildSelectMapping();
		Flux<Map<String, Object>> fromDb = buildFinalSql(mapping, query.where, true, true).execute().fetch().all();
		RowHandler handler = new RowHandler(mapping);
		return Flux.create(sink ->
			fromDb.doOnComplete(() -> handler.handleEnd(sink))
				.subscribe(row -> handler.handleRow(row, sink))
		);
	}
	
	private static class SelectMapping {
		private Map<String, RelationalPersistentEntity<?>> entitiesByAlias = new HashMap<>();
		private Map<String, Table> tableByAlias = new HashMap<>();
		private Map<String, Map<String, String>> fieldAliasesByTableAlias = new HashMap<>();
		private List<SelectField> fields = new LinkedList<>();
		private int aliasCounter = 0;
		
		private String generateAlias() {
			int num = aliasCounter++;
			return "f" + StringUtils.leftPad(Integer.toString(num), 4, '0');
		}
	}
	
	private static class SelectField {
		private String tableAlias;
		private RelationalPersistentProperty property;
		private String fieldAlias;

		public SelectField(String tableAlias, RelationalPersistentProperty property, String fieldAlias) {
			this.tableAlias = tableAlias;
			this.property = property;
			this.fieldAlias = fieldAlias;
		}
		
		public Column toSql() {
			return Column.create(property.getColumnName(), Table.create(property.getOwner().getTableName()).as(tableAlias)).as(fieldAlias);
		}
	}
	
	
	private SelectMapping buildSelectMapping() {
		SelectMapping mapping = new SelectMapping();
		RelationalPersistentEntity<?> entity = client.getMappingContext().getRequiredPersistentEntity(query.from.targetType);
		Map<String, String> fieldAliases = new HashMap<>();
		mapping.fieldAliasesByTableAlias.put(query.from.alias, fieldAliases);
		mapping.entitiesByAlias.put(query.from.alias, entity);
		mapping.tableByAlias.put(query.from.alias, Table.create(entity.getTableName()).as(query.from.alias));
		for (RelationalPersistentProperty property : entity) {
			String alias = mapping.generateAlias();
			mapping.fields.add(new SelectField(query.from.alias, property, alias));
			fieldAliases.put(property.getName(), alias);
		}
		for (TableReference join : query.joins) {
			RelationalPersistentEntity<?> joinEntity = client.getMappingContext().getRequiredPersistentEntity(join.targetType); 
			fieldAliases = new HashMap<>();
			mapping.fieldAliasesByTableAlias.put(join.alias, fieldAliases);
			mapping.entitiesByAlias.put(join.alias, joinEntity);
			mapping.tableByAlias.put(join.alias, Table.create(joinEntity.getTableName()).as(join.alias));
			for (RelationalPersistentProperty property : joinEntity) {
				String alias = mapping.generateAlias();
				mapping.fields.add(new SelectField(join.alias, property, alias));
				fieldAliases.put(property.getName(), alias);
			}
		}
		return mapping;
	}
	
	private SqlQuery<Select> buildFinalSql(SelectMapping mapping, Criteria criteria, boolean applyLimitAndOrderBy, boolean orderById) {
		
		List<Column> selectFields = new ArrayList<>(mapping.fields.size());
		for (SelectField field : mapping.fields)
			selectFields.add(field.toSql());
		BuildSelect select = Select.builder().select(selectFields).from(mapping.tableByAlias.get(query.from.alias));
		if (applyLimitAndOrderBy) {
			select = addLimit(select);
			select = addOrderBy(select);
		}
		
		for (TableReference join : query.joins) {
			select = join(select, join, mapping);
		}

		SqlQuery<Select> q = new SqlQuery<>(client);
		if (criteria != null) {
			select = ((SelectWhere)select).where(criteria.accept(new CriteriaSqlBuilder(mapping.entitiesByAlias, mapping.tableByAlias, q)));
		}
		if (orderById) {
			RelationalPersistentEntity<?> entity = client.getMappingContext().getRequiredPersistentEntity(query.from.targetType);
			if (entity.hasIdProperty()) {
				select = ((SelectOrdered)select).orderBy(
					Column.aliased(
						entity.getRequiredIdProperty().getName(),
						mapping.tableByAlias.get(query.from.alias),
						mapping.fieldAliasesByTableAlias.get(query.from.alias).get(entity.getRequiredIdProperty().getName())
					)
				);
			} else if (entity.isAnnotationPresent(CompositeId.class)) {
				String[] properties = entity.getRequiredAnnotation(CompositeId.class).properties();
				Column[] columns = new Column[properties.length];
				for (int i = 0; i < properties.length; ++i) {
					RelationalPersistentProperty property = entity.getRequiredPersistentProperty(properties[i]);
					columns[i] = Column.aliased(
						property.getName(),
						mapping.tableByAlias.get(query.from.alias),
						mapping.fieldAliasesByTableAlias.get(query.from.alias).get(property.getName())
					);
				}
				select = ((SelectOrdered)select).orderBy(columns);
			}
		}
		
		q.setQuery(select.build());
		return q;
	}
	
	private BuildSelect addLimit(BuildSelect select) {
		if (query.limit > 0) {
			return ((SelectFromAndJoin)select).limitOffset(query.limit, query.offset);
		}
		return select;
	}
	
	private BuildSelect addOrderBy(BuildSelect select) {
		if (!query.orderBy.isEmpty()) {
			List<OrderByField> list = new ArrayList<>(query.orderBy.size());
			for (Tuple3<String, String, Boolean> orderBy : query.orderBy) {
				TableReference table = query.tableAliases.get(orderBy.getT1());
				RelationalPersistentEntity<?> e = client.getMappingContext().getRequiredPersistentEntity(table.targetType);
				RelationalPersistentProperty p = e.getRequiredPersistentProperty(orderBy.getT2());
				OrderByField o = OrderByField.from(Column.create(p.getColumnName(), Table.create(e.getTableName()).as(table.alias)), orderBy.getT3().booleanValue() ? Direction.ASC : Direction.DESC);
				list.add(o);
			}
			return ((SelectFromAndOrderBy)select).orderBy(list);
		}
		return select;
	}

	
	private SqlQuery<Select> buildDistinctRootIdSql(SelectMapping mapping) {
		if ((query.limit > 0 && !query.orderBy.isEmpty()) || hasOrderByOnSubEntityOrOrderByWithConditionOnSubEntity())
			// we need a group by query to handle order by correctly
			return buildDistinctRootIdSqlUsingGroupBy(mapping);
		
		RelationalPersistentEntity<?> entity = client.getMappingContext().getRequiredPersistentEntity(query.from.targetType);
		BuildSelect select = Select.builder()
			.select(Column.create(entity.getIdColumn(), mapping.tableByAlias.get(query.from.alias)))
			.distinct()
			.from(mapping.tableByAlias.get(query.from.alias));
		select = addLimit(select);
		select = addOrderBy(select);
		
		for (TableReference join : query.joins) {
			if (!needsTableForPreSelect(join, true))
				continue;
			select = join(select, join, mapping);
		}

		SqlQuery<Select> q = new SqlQuery<>(client);
		if (query.where != null) {
			select = ((SelectWhere)select).where(query.where.accept(new CriteriaSqlBuilder(mapping.entitiesByAlias, mapping.tableByAlias, q)));
		}
		
		q.setQuery(select.build());
		return q;
	}
	
	private SqlQuery<Select> buildDistinctRootIdSqlUsingGroupBy(SelectMapping mapping) {
		RelationalPersistentEntity<?> entity = client.getMappingContext().getRequiredPersistentEntity(query.from.targetType);
		BuildSelect select = Select.builder()
			.select(Column.create(entity.getIdColumn(), mapping.tableByAlias.get(query.from.alias)))
			.from(mapping.tableByAlias.get(query.from.alias))
			;
		
		for (TableReference join : query.joins) {
			if (!needsTableForPreSelect(join, true))
				continue;
			select = join(select, join, mapping);
		}

		SqlQuery<Select> q = new SqlQuery<>(client) {
			@Override
			protected String finalizeQuery(String sql) {
				StringBuilder s = new StringBuilder(sql);
				s.append(" GROUP BY ").append(Column.create(entity.getIdColumn(), mapping.tableByAlias.get(query.from.alias)));
				s.append(" ORDER BY ");
				for (Tuple3<String, String, Boolean> orderBy : query.orderBy) {
					TableReference table = query.tableAliases.get(orderBy.getT1());
					RelationalPersistentEntity<?> e = client.getMappingContext().getRequiredPersistentEntity(table.targetType);
					RelationalPersistentProperty p = e.getRequiredPersistentProperty(orderBy.getT2());
					Column col = Column.create(p.getColumnName(), Table.create(e.getTableName()).as(table.alias));
					if (orderBy.getT3().booleanValue()) {
						s.append("MIN(").append(col).append(") ASC");
					} else {
						s.append("MAX(").append(col).append(") DESC");
					}
				}
				if (query.limit > 0) {
					s.append(" LIMIT ").append(query.limit).append(" OFFSET ").append(query.offset);
				}
				return s.toString();
			}
		};
		if (query.where != null) {
			select = ((SelectWhere)select).where(query.where.accept(new CriteriaSqlBuilder(mapping.entitiesByAlias, mapping.tableByAlias, q)));
		}
		
		q.setQuery(select.build());
		return q;
	}
	
	private BuildSelect join(BuildSelect select, TableReference join, SelectMapping mapping) {
		RelationalPersistentEntity<?> sourceEntity = client.getMappingContext().getRequiredPersistentEntity(join.source.targetType); 
		RelationalPersistentEntity<?> targetEntity = client.getMappingContext().getRequiredPersistentEntity(join.targetType); 
		RelationalPersistentProperty property = sourceEntity.getPersistentProperty(join.propertyName);
		if (property != null) {
			Table joinTargetTable = mapping.tableByAlias.get(join.alias);
			Column joinTarget = Column.create(targetEntity.getIdColumn(), joinTargetTable);
			Table joinSourceTable = mapping.tableByAlias.get(join.source.alias);
			Column joinSource = Column.create(property.getColumnName(), joinSourceTable);
			return ((SelectJoin)select).leftOuterJoin(joinTargetTable).on(joinTarget).equals(joinSource);
		}
		ForeignTable ft = LcEntityTypeInfo.get(join.source.targetType).getRequiredForeignTableForProperty(join.propertyName);
		property = targetEntity.getRequiredPersistentProperty(ft.joinKey());

		Table joinTargetTable = mapping.tableByAlias.get(join.alias);
		Column joinTarget = Column.create(property.getColumnName(), joinTargetTable);
		Table joinSourceTable = mapping.tableByAlias.get(join.source.alias);
		Column joinSource = Column.create(sourceEntity.getIdColumn(), joinSourceTable);
		return ((SelectJoin)select).leftOuterJoin(joinTargetTable).on(joinTarget).equals(joinSource);
	}
	
	private class RowHandler {
	
		private SelectMapping mapping;
		private RelationalPersistentEntity<?> rootEntity;
		private Map<String, String> rootAliases;
		private T currentRoot = null;
		private Object currentRootId = null;
		
		private RowHandler(SelectMapping mapping) {
			this.mapping = mapping;
			this.rootEntity = client.getMappingContext().getRequiredPersistentEntity(query.from.targetType);
			this.rootAliases = mapping.fieldAliasesByTableAlias.get(query.from.alias);
		}
		
		@SuppressWarnings("unchecked")
		private void handleRow(Map<String, Object> row, FluxSink<T> sink) {
			if (logger.isDebugEnabled())
				logger.debug("Result row = " + row);
			PropertiesSource source = new PropertiesSourceMap(row, rootAliases);
			Object rootId = ModelUtils.getId(rootEntity, source);
			if (currentRoot != null) {
				if (rootId != null && !currentRootId.equals(rootId)) {
					endOfRoot();
					sink.next(currentRoot);
					currentRoot = (T) reader.read(query.from.targetType, source);
					currentRootId = rootId;
				}
			} else {
				currentRoot = (T) reader.read(query.from.targetType, source);
				currentRootId = rootId;
			}
			fillLinkedEntities(currentRoot, EntityState.get(currentRoot, client, rootEntity), query.from, row);
		}
		
		private void handleEnd(FluxSink<T> sink) {
			if (logger.isDebugEnabled())
				logger.debug("End of rows");
			if (currentRoot != null) {
				endOfRoot();
				sink.next(currentRoot);
			}
			sink.complete();
		}
	
		private void fillLinkedEntities(Object parent, EntityState parentState, TableReference parentTable, Map<String, Object> row) {
			for (TableReference join : query.joins) {
				if (join.source != parentTable)
					continue;
				try {
					fillLinkedEntity(join, parent, parentState, row);
				} catch (Exception e) {
					throw new MappingException("Error mapping result for entity " + join.targetType.getName(), e);
				}
			}
		}
		
		@SuppressWarnings({"java:S3011", "unchecked"}) // access directly to field
		private <J> void fillLinkedEntity(TableReference join, Object parent, EntityState parentState, Map<String, Object> row) throws ReflectiveOperationException {
			if (logger.isDebugEnabled())
				logger.debug("Read join " + join.targetType.getSimpleName() + " as " + join.alias + " from " + parent.getClass().getSimpleName());
			Field field = parent.getClass().getDeclaredField(join.propertyName);
			field.setAccessible(true);
			Class<?> type;
			boolean isCollection = ModelUtils.isCollection(field);
			if (isCollection)
				type = ModelUtils.getRequiredCollectionType(field);
			else
				type = field.getType();
			RelationalPersistentEntity<?> entity = client.getMappingContext().getRequiredPersistentEntity(type);
			PropertiesSource source = new PropertiesSourceMap(row, mapping.fieldAliasesByTableAlias.get(join.alias));
			Object id = ModelUtils.getId(entity, source);
			if (id == null) {
				// left join without any match
				if (isCollection)
					field.set(parent, CollectionFactory.createCollection(field.getType(), ModelUtils.getCollectionType(field), 0));
				return;
			}
			J instance = reader.read((Class<J>) join.targetType, source);
			if (isCollection)
				ModelUtils.addToCollectionField(field, parent, instance);
			else {
				if (LcEntityTypeInfo.isForeignTableField(field))
					parentState.setForeignTableField(parent, field, instance, true);
				else
					parentState.setPersistedField(parent, field, instance, true);
			}
			fillLinkedEntities(instance, EntityState.get(instance, client, entity), join, row);
		}
		
		
		private void endOfRoot() {
			signalLoadedForeignTables(currentRoot, query.from);
		}
	
		private void signalLoadedForeignTables(Object parent, TableReference parentTable) {
			for (TableReference join : query.joins) {
				if (join.source != parentTable)
					continue;
				try {
					signalLoadedForeignTable(parent, join);
				} catch (Exception e) {
					throw new MappingException("Error mapping result for entity " + join.targetType.getName(), e);
				}
			}
		}
		
		@SuppressWarnings("squid:S3011")
		private void signalLoadedForeignTable(Object parent, TableReference join) throws ReflectiveOperationException {
			Field field = parent.getClass().getDeclaredField(join.propertyName);
			field.setAccessible(true);
			Object instance = field.get(parent);
			if (field.isAnnotationPresent(ForeignTable.class)) {
				EntityState.get(parent, client).foreignTableLoaded(field, instance);
			}
			if (instance != null) {
				boolean isCollection = ModelUtils.isCollection(field);
				if (isCollection)
					for (Object element : ModelUtils.getAsCollection(instance))
						signalLoadedForeignTables(element, join);
				else
					signalLoadedForeignTables(instance, join);
			}
		}
	}
}
