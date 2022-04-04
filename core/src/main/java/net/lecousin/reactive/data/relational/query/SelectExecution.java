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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mapping.MappingException;
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
import net.lecousin.reactive.data.relational.mapping.LcEntityReader;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.model.PropertiesSource;
import net.lecousin.reactive.data.relational.model.PropertiesSourceMap;
import net.lecousin.reactive.data.relational.model.metadata.EntityInstance;
import net.lecousin.reactive.data.relational.model.metadata.EntityMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;
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

/**
 * Orchestrate the execution of a {@link SelectQuery}
 * 
 * @author Guillaume Le Cousin
 *
 * @param <T> type of entity
 */
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
		EntityMetadata meta = client.getRequiredEntity(query.from.targetType);
		SelectMapping mapping = buildSelectMapping();
		List<Expression> idColumns;
		if (meta.hasIdProperty()) {
			idColumns = Collections.singletonList(Column.create(meta.getRequiredIdProperty().getColumnName(), mapping.tableByAlias.get(query.from.alias)));
		} else if (meta.hasCompositeId()) {
			List<PropertyMetadata> properties = meta.getCompositeIdProperties();
			idColumns = new ArrayList<>(properties.size());
			for (PropertyMetadata property : properties)
				idColumns.add(Column.create(property.getColumnName(), mapping.tableByAlias.get(query.from.alias)));
		} else {
			throw new IllegalArgumentException("Cannot count distinct entities without an Id column or a CompoisteId");
		}
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
	
	private static boolean isMany(TableReference table) {
		if (table.source == null)
			return false;
		try {
			Field field = table.source.targetType.getDeclaredField(table.propertyName);
			return ModelUtils.isCollection(field);
		} catch (Exception e) {
			return false;
		}
	}
	
	private static boolean isManyFromRoot(TableReference table) {
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
				if (logger.isDebugEnabled())
					logger.debug("Pre-selected ids bunch: " + Objects.toString(ids));
				String idPropertyName = mapping.entitiesByAlias.get(query.from.alias).getRequiredIdProperty().getName();
				Flux<Map<String, Object>> fromDb = buildFinalSql(mapping, Criteria.property(query.from.alias, idPropertyName).in(ids), false, true).execute().fetch().all();
				return Flux.create((Consumer<FluxSink<T>>)sink -> {
					RowHandlerSorted handler = new RowHandlerSorted(mapping, sink, ids);
					fromDb.doOnComplete(handler::handleEnd).subscribe(handler::handleRow, sink::error);
				});
			});
	}
	
	private Flux<T> executeWithoutPreSelect() {
		SelectMapping mapping = buildSelectMapping();
		Flux<Map<String, Object>> fromDb = buildFinalSql(mapping, query.where, true, hasJoinMany()).execute().fetch().all();
		return Flux.create((Consumer<FluxSink<T>>)sink -> {
			RowHandler handler = new RowHandler(mapping, sink);
			fromDb.doOnComplete(handler::handleEnd).subscribe(handler::handleRow, sink::error);
		});
	}
	
	private static class SelectMapping {
		private Map<String, EntityMetadata> entitiesByAlias = new HashMap<>();
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
		private PropertyMetadata property;
		private String fieldAlias;

		public SelectField(String tableAlias, PropertyMetadata property, String fieldAlias) {
			this.tableAlias = tableAlias;
			this.property = property;
			this.fieldAlias = fieldAlias;
		}
		
		public Column toSql() {
			return Column.create(property.getColumnName(), Table.create(property.getEntity().getTableName()).as(tableAlias)).as(fieldAlias);
		}
	}
	
	
	private SelectMapping buildSelectMapping() {
		SelectMapping mapping = new SelectMapping();
		EntityMetadata meta = client.getRequiredEntity(query.from.targetType);
		Map<String, String> fieldAliases = new HashMap<>();
		mapping.fieldAliasesByTableAlias.put(query.from.alias, fieldAliases);
		mapping.entitiesByAlias.put(query.from.alias, meta);
		mapping.tableByAlias.put(query.from.alias, Table.create(meta.getTableName()).as(query.from.alias));
		for (PropertyMetadata property : meta.getPersistentProperties()) {
			String alias = mapping.generateAlias();
			mapping.fields.add(new SelectField(query.from.alias, property, alias));
			fieldAliases.put(property.getName(), alias);
		}
		for (TableReference join : query.joins) {
			EntityMetadata joinEntity = client.getRequiredEntity(join.targetType); 
			fieldAliases = new HashMap<>();
			mapping.fieldAliasesByTableAlias.put(join.alias, fieldAliases);
			mapping.entitiesByAlias.put(join.alias, joinEntity);
			mapping.tableByAlias.put(join.alias, Table.create(joinEntity.getTableName()).as(join.alias));
			for (PropertyMetadata property : joinEntity.getPersistentProperties()) {
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
			EntityMetadata entity = client.getRequiredEntity(query.from.targetType);
			if (entity.hasIdProperty()) {
				String idPropertyName = entity.getRequiredIdProperty().getName();
				select = ((SelectOrdered)select).orderBy(
					Column.aliased(
						idPropertyName,
						mapping.tableByAlias.get(query.from.alias),
						mapping.fieldAliasesByTableAlias.get(query.from.alias).get(idPropertyName)
					)
				);
			} else if (entity.hasCompositeId()) {
				List<PropertyMetadata> properties = entity.getCompositeIdProperties();
				Column[] columns = new Column[properties.size()];
				int i = 0;
				for (PropertyMetadata property : properties) {
					columns[i++] = Column.aliased(
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
				EntityMetadata e = client.getRequiredEntity(table.targetType);
				PropertyMetadata p = e.getRequiredPersistentProperty(orderBy.getT2());
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
		
		EntityMetadata entity = client.getRequiredEntity(query.from.targetType);
		BuildSelect select = Select.builder()
			.select(Column.create(entity.getRequiredIdProperty().getColumnName(), mapping.tableByAlias.get(query.from.alias)))
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
		EntityMetadata entity = client.getRequiredEntity(query.from.targetType);
		BuildSelect select = Select.builder()
			.select(Column.create(entity.getRequiredIdProperty().getColumnName(), mapping.tableByAlias.get(query.from.alias)))
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
				s.append(" GROUP BY ").append(Column.create(entity.getRequiredIdProperty().getColumnName(), mapping.tableByAlias.get(query.from.alias)));
				s.append(" ORDER BY ");
				for (Tuple3<String, String, Boolean> orderBy : query.orderBy) {
					TableReference table = query.tableAliases.get(orderBy.getT1());
					EntityMetadata e = client.getRequiredEntity(table.targetType);
					PropertyMetadata p = e.getRequiredPersistentProperty(orderBy.getT2());
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
		EntityMetadata sourceEntity = client.getRequiredEntity(join.source.targetType); 
		EntityMetadata targetEntity = client.getRequiredEntity(join.targetType); 
		PropertyMetadata property = sourceEntity.getProperty(join.propertyName);
		if (property != null && property.isPersistent()) {
			Table joinTargetTable = mapping.tableByAlias.get(join.alias);
			Column joinTarget = Column.create(targetEntity.getRequiredIdProperty().getColumnName(), joinTargetTable);
			Table joinSourceTable = mapping.tableByAlias.get(join.source.alias);
			Column joinSource = Column.create(property.getColumnName(), joinSourceTable);
			return ((SelectJoin)select).leftOuterJoin(joinTargetTable).on(joinTarget).equals(joinSource);
		}
		PropertyMetadata foreignTable = sourceEntity.getRequiredForeignTableProperty(join.propertyName);
		property = targetEntity.getRequiredPersistentProperty(foreignTable.getForeignTableAnnotation().joinKey());

		Table joinTargetTable = mapping.tableByAlias.get(join.alias);
		Column joinTarget = Column.create(property.getColumnName(), joinTargetTable);
		Table joinSourceTable = mapping.tableByAlias.get(join.source.alias);
		Column joinSource = Column.create(sourceEntity.getRequiredIdProperty().getColumnName(), joinSourceTable);
		return ((SelectJoin)select).leftOuterJoin(joinTargetTable).on(joinTarget).equals(joinSource);
	}

	private static class JoinStatus {
		private EntityMetadata entityType;
		private Function<PropertiesSource, Object> idGetter;
		private Map<String, String> aliases;
		
		private PropertyMetadata joinProperty;
		
		private List<JoinStatus> joins;
		
		private EntityInstance<?> currentEntityInstance = null;
		private Object currentId = null;
		
		@SuppressWarnings("java:S3011")
		private JoinStatus(TableReference table, TableReference fromJoin, SelectMapping mapping, List<TableReference> allJoins, LcReactiveDataRelationalClient client) throws ReflectiveOperationException {
			this.entityType = client.getRequiredEntity(table.targetType);
			this.idGetter = ModelUtils.idGetter(entityType.getSpringMetadata());
			this.aliases = mapping.fieldAliasesByTableAlias.get(table.alias);
			
			if (fromJoin != null)
				joinProperty = client.getRequiredEntity(fromJoin.targetType).getRequiredProperty(table.propertyName);
			
			this.joins = new LinkedList<>();
			for (TableReference join : allJoins) {
				if (join.source == table) {
					this.joins.add(new JoinStatus(join, table, mapping, allJoins, client));
				}
			}
		}
		
		private void reset(EntityInstance<?> parentInstance) {
			if (currentEntityInstance == null)
				return;

			if (parentInstance != null && joinProperty.isForeignTable())
				parentInstance.getState().foreignTableLoaded(joinProperty.getStaticMetadata().getField(), ModelUtils.getFieldValue(parentInstance.getEntity(), joinProperty.getStaticMetadata().getField()));

			for (JoinStatus join : joins)
				join.reset(currentEntityInstance);
			
			currentEntityInstance = null;
			currentId = null;
		}
		
		private void readNewInstance(Object id, PropertiesSource source, LcEntityReader reader) {
			reset(null);
			currentId = id;
			if (id != null) {
				currentEntityInstance = reader.getCache().getInstanceById(entityType.getType(), id);
				if (currentEntityInstance != null && !currentEntityInstance.getState().isLoaded())
					currentEntityInstance = null;
			}
			if (currentEntityInstance == null) {
				currentEntityInstance = reader.read(entityType, source);
			}
		}
	}
	
	private class RowHandler {
		
		protected JoinStatus rootStatus;
		protected FluxSink<T> sink;
		
		protected RowHandler(SelectMapping mapping, FluxSink<T> sink) {
			this.sink = sink;
			try {
				this.rootStatus = new JoinStatus(query.from, null, mapping, query.joins, client);
			} catch (Exception e) {
				throw new MappingException("Error initializing row mapper", e);
			}
		}
		
		public void handleRow(Map<String, Object> row) {
			if (logger.isDebugEnabled())
				logger.debug("Result row = " + row);
			PropertiesSource source = new PropertiesSourceMap(row, rootStatus.aliases);
			Object rootId = rootStatus.idGetter.apply(source);
			if (rootStatus.currentEntityInstance != null) {
				if (rootId != null && !rootStatus.currentId.equals(rootId)) {
					@SuppressWarnings("unchecked")
					EntityInstance<T> instance = (EntityInstance<T>) rootStatus.currentEntityInstance;
					Object id = rootStatus.currentId;
					rootStatus.reset(null);
					newRootReady(instance.getEntity(), id);
					rootStatus.readNewInstance(rootId, source, reader);
				}
			} else {
				rootStatus.readNewInstance(rootId, source, reader);
			}
			fillLinkedEntities(rootStatus, row);
		}
		
		public void handleEnd() {
			if (logger.isDebugEnabled())
				logger.debug("End of rows");
			if (rootStatus.currentEntityInstance != null) {
				@SuppressWarnings("unchecked")
				EntityInstance<T> instance = (EntityInstance<T>) rootStatus.currentEntityInstance;
				Object id = rootStatus.currentId;
				rootStatus.reset(null);
				newRootReady(instance.getEntity(), id);
			}
			endOfRoots();
		}
		
		protected void newRootReady(T root, @SuppressWarnings({"unused", "java:S1172"}) Object rootId) {
			sink.next(root);
		}
		
		protected void endOfRoots() {
			sink.complete();
		}
	
		private void fillLinkedEntities(JoinStatus parent, Map<String, Object> row) {
			for (JoinStatus join : parent.joins) {
				try {
					fillLinkedEntity(join, parent, row);
				} catch (Exception e) {
					throw new MappingException("Error mapping result for entity " + join.entityType.getType().getName(), e);
				}
			}
		}
		
		@SuppressWarnings("java:S3011")
		private void fillLinkedEntity(JoinStatus join, JoinStatus parent, Map<String, Object> row) throws ReflectiveOperationException {
			if (logger.isDebugEnabled())
				logger.debug("Read join " + join.entityType.getType().getSimpleName() + " from " + parent.entityType.getType().getSimpleName());
			PropertiesSource source = new PropertiesSourceMap(row, join.aliases);
			Object id = join.idGetter.apply(source);
			if (id == null) {
				// left join without any match
				if (join.joinProperty.isCollection() && ModelUtils.getFieldValue(parent.currentEntityInstance.getEntity(), join.joinProperty.getStaticMetadata().getField()) == null)
					ModelUtils.setFieldValue(
						parent.currentEntityInstance.getEntity(),
						join.joinProperty.getStaticMetadata().getField(),
						CollectionFactory.createCollection(join.joinProperty.getType(), ModelUtils.getCollectionType(join.joinProperty.getStaticMetadata().getField()), 0)
					);
				return;
			}
			if (!id.equals(join.currentId)) {
				join.readNewInstance(id, source, reader);
				if (join.joinProperty.isCollection())
					ModelUtils.addToCollectionField(join.joinProperty.getStaticMetadata().getField(), parent.currentEntityInstance.getEntity(), join.currentEntityInstance.getEntity());
				else {
					if (join.joinProperty.isForeignTable())
						parent.currentEntityInstance.getState().setForeignTableField(parent.currentEntityInstance.getEntity(), join.joinProperty.getStaticMetadata(), join.currentEntityInstance.getEntity());
					else
						ModelUtils.setFieldValue(parent.currentEntityInstance.getEntity(), join.joinProperty.getStaticMetadata().getField(), join.currentEntityInstance.getEntity());
				}
			}
			fillLinkedEntities(join, row);
		}
		
	}
	
	private class RowHandlerSorted extends RowHandler {
		
		private LinkedList<Object> sortedIds;
		private Map<Object, T> waitingInstances = new HashMap<>();
		
		private RowHandlerSorted(SelectMapping mapping, FluxSink<T> sink, List<Object> sortedIds) {
			super(mapping, sink);
			this.sortedIds = new LinkedList<>(sortedIds);
		}
		
		@Override
		protected void newRootReady(T root, Object rootId) {
			Object nextId = sortedIds.getFirst();
			if (!rootId.equals(nextId)) {
				waitingInstances.put(rootId, root);
				return;
			}
			sink.next(root);
			sortedIds.removeFirst();
			while (!sortedIds.isEmpty() && !waitingInstances.isEmpty()) {
				nextId = sortedIds.getFirst();
				T instance = waitingInstances.remove(nextId);
				if (instance == null)
					break;
				sink.next(instance);
				sortedIds.removeFirst();
			}
		}
		
		@Override
		protected void endOfRoots() {
			while (!waitingInstances.isEmpty()) {
				Object nextId = sortedIds.removeFirst();
				T instance = waitingInstances.get(nextId);
				if (instance != null)
					sink.next(instance);
			}
			sink.complete();
		}
		
	}
}
