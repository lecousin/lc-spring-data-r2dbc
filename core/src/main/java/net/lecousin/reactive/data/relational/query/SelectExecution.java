package net.lecousin.reactive.data.relational.query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.data.r2dbc.core.PreparedOperation;
import org.springframework.data.r2dbc.dialect.BindMarker;
import org.springframework.data.r2dbc.dialect.BindMarkers;
import org.springframework.data.r2dbc.dialect.BindTarget;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.SelectBuilder.BuildSelect;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectFromAndJoin;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectJoin;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectOrdered;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectWhere;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

import io.r2dbc.spi.Row;
import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.mapping.LcMappingR2dbcConverter;
import net.lecousin.reactive.data.relational.mapping.ResultMappingContext;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.query.SelectQuery.TableReference;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.And;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.Or;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.PropertyOperand;
import net.lecousin.reactive.data.relational.query.criteria.Criteria.PropertyOperation;
import net.lecousin.reactive.data.relational.query.criteria.CriteriaSqlBuilder;
import net.lecousin.reactive.data.relational.query.criteria.CriteriaVisitor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

public class SelectExecution<T> {
	
	private static final Log logger = LogFactory.getLog(SelectExecution.class);
	
	private SelectQuery<T> query;
	private LcReactiveDataRelationalClient client;
	private R2dbcDialect dialect;
	private RenderContext renderContext;
	private LcMappingR2dbcConverter mapper;
	
	public SelectExecution(SelectQuery<T> query, LcReactiveDataRelationalClient client) {
		this.query = query;
		this.client = client;
		this.dialect = client.getDataAccess().getDialect();
		this.renderContext = client.getDataAccess().getStatementMapper().getRenderContext();
		this.mapper = client.getMapper();
	}
	
	public Flux<T> execute() {
		return Mono.fromCallable(this::needsPreSelectIds)
			.flatMapMany(needsPreSelect -> needsPreSelect.booleanValue() ? executeWithPreSelect() : executeWithoutPreSelect());
	}
	
	private boolean needsPreSelectIds() {
		// first step is to ensure we wave the target type for all joins
		query.setJoinsTargetType(mapper);
		if (!hasJoinMany())
			return false;
		if (query.limit > 0)
			return true;
		return hasConditionOnManyEntity();
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
		RelationalPersistentEntity<?> entity = mapper.getMappingContext().getRequiredPersistentEntity(table.source.targetType);
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
	
	private boolean hasConditionOnManyEntity() {
		if (query.where == null)
			return false;
		Boolean found = query.where.accept(new CriteriaVisitor.DefaultVisitor<Boolean>() {
			@Override
			public Boolean visit(And and) {
				return Boolean.valueOf(and.getLeft().accept(this).booleanValue() && and.getRight().accept(this).booleanValue());
			}
			@Override
			public Boolean visit(Or or) {
				return Boolean.valueOf(or.getLeft().accept(this).booleanValue() && or.getRight().accept(this).booleanValue());
			}
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
	
	private boolean needsTableForPreSelect(TableReference table) {
		if (query.where == null)
			return false;
		Boolean found = query.where.accept(new CriteriaVisitor.DefaultVisitor<Boolean>() {
			@Override
			public Boolean visit(And and) {
				return Boolean.valueOf(and.getLeft().accept(this).booleanValue() && and.getRight().accept(this).booleanValue());
			}
			@Override
			public Boolean visit(Or or) {
				return Boolean.valueOf(or.getLeft().accept(this).booleanValue() && or.getRight().accept(this).booleanValue());
			}
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
		ResultMappingContext resultContext = new ResultMappingContext();
		return buildDistinctRootIdSql(mapping).execute(client.getSpringClient(), renderContext).fetch().all()
			.map(row -> row.values().iterator().next())
			.buffer(100)
			.flatMap(ids -> {
				String idPropertyName = mapping.entitiesByAlias.get(query.from.alias).getIdProperty().getName();
				Flux<Map<String, Object>> fromDb = buildFinalSql(mapping, Criteria.property(query.from.alias, idPropertyName).in(ids), false).execute(client.getSpringClient(), renderContext).fetch().all();
				return Flux.create(sink ->
					fromDb.doOnComplete(() -> handleRow(null, sink, mapping, resultContext))
						.subscribe(row -> handleRow(row, sink, mapping, resultContext))
				);
			});
	}
	
	private Flux<T> executeWithoutPreSelect() {
		SelectMapping mapping = buildSelectMapping();
		ResultMappingContext resultContext = new ResultMappingContext();
		Flux<Map<String, Object>> fromDb = buildFinalSql(mapping, query.where, true).execute(client.getSpringClient(), renderContext).fetch().all();
		return Flux.create(sink ->
			fromDb.doOnComplete(() -> handleRow(null, sink, mapping, resultContext))
				.subscribe(row -> handleRow(row, sink, mapping, resultContext))
		);
	}
	
	private static class SelectMapping {
		private Map<String, RelationalPersistentEntity<?>> entitiesByAlias = new HashMap<>();
		private Map<String, Table> tableByAlias = new HashMap<>();
		private Map<String, Map<String, String>> fieldAliasesByTableAlias = new HashMap<>();
		private List<SelectField> fields = new LinkedList<>();
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
		RelationalPersistentEntity<?> entity = mapper.getMappingContext().getRequiredPersistentEntity(query.from.targetType);
		Map<String, String> fieldAliases = new HashMap<>();
		mapping.fieldAliasesByTableAlias.put(query.from.alias, fieldAliases);
		mapping.entitiesByAlias.put(query.from.alias, entity);
		mapping.tableByAlias.put(query.from.alias, Table.create(entity.getTableName()).as(query.from.alias));
		for (RelationalPersistentProperty property : entity) {
			String alias = "f" + mapping.fields.size();
			mapping.fields.add(new SelectField(query.from.alias, property, alias));
			fieldAliases.put(property.getName(), alias);
		}
		for (TableReference join : query.joins) {
			RelationalPersistentEntity<?> joinEntity = mapper.getMappingContext().getRequiredPersistentEntity(join.targetType); 
			fieldAliases = new HashMap<>();
			mapping.fieldAliasesByTableAlias.put(join.alias, fieldAliases);
			mapping.entitiesByAlias.put(join.alias, joinEntity);
			mapping.tableByAlias.put(join.alias, Table.create(joinEntity.getTableName()).as(join.alias));
			for (RelationalPersistentProperty property : joinEntity) {
				String alias = "f" + mapping.fields.size();
				mapping.fields.add(new SelectField(join.alias, property, alias));
				fieldAliases.put(property.getName(), alias);
			}
		}
		return mapping;
	}
	
	private static class SqlQuery {
		private Select query;
		private BindMarkers markers;
		private Map<BindMarker, Object> bindings = new HashMap<>();
		
		private GenericExecuteSpec execute(DatabaseClient client, RenderContext renderContext) {
			PreparedOperation<Select> operation = new PreparedOperation<Select>() {
				@Override
				public Select getSource() {
					return query;
				}
				
				@Override
				public void bindTo(BindTarget target) {
					for (Map.Entry<BindMarker, Object> binding : bindings.entrySet())
						binding.getKey().bind(target, binding.getValue());
				}
				
				@Override
				public String toQuery() {
					return SqlRenderer.create(renderContext).render(query);
				}
			};
			return client.execute(operation);
		}
		
	}
	
	private SqlQuery buildFinalSql(SelectMapping mapping, Criteria criteria, boolean applyLimit) {
		RelationalPersistentEntity<?> entity = mapper.getMappingContext().getPersistentEntity(query.from.targetType);
		
		List<Column> selectFields = new ArrayList<>(mapping.fields.size());
		for (SelectField field : mapping.fields)
			selectFields.add(field.toSql());
		BuildSelect select = Select.builder().select(selectFields).from(mapping.tableByAlias.get(query.from.alias));
		if (applyLimit && query.limit > 0) {
			select = ((SelectFromAndJoin)select).limitOffset(query.limit, query.offset);
		}
		
		for (TableReference join : query.joins) {
			RelationalPersistentEntity<?> sourceEntity = mapper.getMappingContext().getRequiredPersistentEntity(join.source.targetType); 
			RelationalPersistentEntity<?> targetEntity = mapper.getMappingContext().getRequiredPersistentEntity(join.targetType); 
			RelationalPersistentProperty property = sourceEntity.getPersistentProperty(join.propertyName);
			if (property != null) {
				Table joinTargetTable = mapping.tableByAlias.get(join.alias);
				Column joinTarget = Column.create(targetEntity.getIdColumn(), joinTargetTable);
				Table joinSourceTable = mapping.tableByAlias.get(join.source.alias);
				Column joinSource = Column.create(property.getColumnName(), joinSourceTable);
				select = ((SelectJoin)select).leftOuterJoin(joinTargetTable).on(joinTarget).equals(joinSource);
			} else {
				ForeignTable ft = ModelUtils.getRequiredForeignTableForProperty(join.source.targetType, join.propertyName);
				property = targetEntity.getRequiredPersistentProperty(ft.joinKey());

				Table joinTargetTable = mapping.tableByAlias.get(join.alias);
				Column joinTarget = Column.create(property.getColumnName(), joinTargetTable);
				Table joinSourceTable = mapping.tableByAlias.get(join.source.alias);
				Column joinSource = Column.create(sourceEntity.getIdColumn(), joinSourceTable);
				select = ((SelectJoin)select).leftOuterJoin(joinTargetTable).on(joinTarget).equals(joinSource);
			}
		}

		SqlQuery q = new SqlQuery();
		q.markers = dialect.getBindMarkersFactory().create();
		if (criteria != null) {
			select = ((SelectWhere)select).where(criteria.accept(new CriteriaSqlBuilder(mapping.entitiesByAlias, mapping.tableByAlias, q.markers, q.bindings)));
		}
		if (entity.hasIdProperty()) {
			select = ((SelectOrdered)select).orderBy(Column.create(mapping.fieldAliasesByTableAlias.get(query.from.alias).get(entity.getRequiredIdProperty().getName()), mapping.tableByAlias.get(query.from.alias)));
		}
		
		q.query = select.build();
		return q;
	}

	
	private SqlQuery buildDistinctRootIdSql(SelectMapping mapping) {
		RelationalPersistentEntity<?> entity = mapper.getMappingContext().getRequiredPersistentEntity(query.from.targetType);
		
		BuildSelect select = Select.builder().select(Column.create(entity.getIdColumn(), mapping.tableByAlias.get(query.from.alias))).distinct().from(mapping.tableByAlias.get(query.from.alias));
		if (query.limit > 0) {
			select = ((SelectFromAndJoin)select).limitOffset(query.limit, query.offset);
		}
		
		for (TableReference join : query.joins) {
			if (!needsTableForPreSelect(join))
				continue;
			RelationalPersistentEntity<?> sourceEntity = mapper.getMappingContext().getRequiredPersistentEntity(join.source.targetType); 
			RelationalPersistentEntity<?> targetEntity = mapper.getMappingContext().getRequiredPersistentEntity(join.targetType); 
			RelationalPersistentProperty property = sourceEntity.getPersistentProperty(join.propertyName);
			if (property != null) {
				Table joinTargetTable = mapping.tableByAlias.get(join.alias);
				Column joinTarget = Column.create(targetEntity.getIdColumn(), joinTargetTable);
				Table joinSourceTable = mapping.tableByAlias.get(join.source.alias);
				Column joinSource = Column.create(property.getColumnName(), joinSourceTable);
				select = ((SelectJoin)select).leftOuterJoin(joinTargetTable).on(joinTarget).equals(joinSource);
			} else {
				ForeignTable ft = ModelUtils.getRequiredForeignTableForProperty(join.source.targetType, join.propertyName);
				property = targetEntity.getRequiredPersistentProperty(ft.joinKey());

				Table joinTargetTable = mapping.tableByAlias.get(join.alias);
				Column joinTarget = Column.create(property.getColumnName(), joinTargetTable);
				Table joinSourceTable = mapping.tableByAlias.get(join.source.alias);
				Column joinSource = Column.create(sourceEntity.getIdColumn(), joinSourceTable);
				select = ((SelectJoin)select).leftOuterJoin(joinTargetTable).on(joinTarget).equals(joinSource);
			}
		}

		SqlQuery q = new SqlQuery();
		q.markers = dialect.getBindMarkersFactory().create();
		if (query.where != null) {
			select = ((SelectWhere)select).where(query.where.accept(new CriteriaSqlBuilder(mapping.entitiesByAlias, mapping.tableByAlias, q.markers, q.bindings)));
		}
		
		q.query = select.build();
		return q;
	}
	
	private T currentRoot = null;
	private Object currentRootId = null;
	
	@SuppressWarnings("unchecked")
	private void handleRow(Map<String, Object> row, FluxSink<T> sink, SelectMapping mapping, ResultMappingContext resultContext) {
		if (logger.isDebugEnabled())
			logger.debug("Result row = " + row);
		if (row == null) {
			if (currentRoot != null) {
				endOfRoot();
				sink.next(currentRoot);
			}
			sink.complete();
			return;
		}
		RelationalPersistentEntity<?> rootEntity = mapper.getMappingContext().getRequiredPersistentEntity(query.from.targetType);
		Object rootId = ModelUtils.getId(row, rootEntity, name -> mapping.fieldAliasesByTableAlias.get(query.from.alias).get(name));
		if (currentRoot != null) {
			if (rootId != null && !currentRootId.equals(rootId)) {
				endOfRoot();
				sink.next(currentRoot);
				currentRoot = (T) mapper.read(query.from.targetType, createRow(row, rootEntity, query.from.alias, mapping), null, resultContext);
				currentRootId = rootId;
			}
		} else {
			currentRoot = (T) mapper.read(query.from.targetType, createRow(row, rootEntity, query.from.alias, mapping), null, resultContext);
			currentRootId = rootId;
		}
		fillLinkedEntities(currentRoot, query.from, row, mapping, resultContext);
	}
	
	@SuppressWarnings({"java:S3011"}) // access directly to field
	private void fillLinkedEntities(Object parent, TableReference parentTable, Map<String, Object> row, SelectMapping mapping, ResultMappingContext resultContext) {
		for (TableReference join : query.joins) {
			if (join.source != parentTable)
				continue;
			try {
				Field field = parent.getClass().getDeclaredField(join.propertyName);
				field.setAccessible(true);
				Class<?> type;
				boolean isCollection = ModelUtils.isCollection(field);
				if (isCollection)
					type = ModelUtils.getRequiredCollectionType(field);
				else
					type = field.getType();
				RelationalPersistentEntity<?> entity = mapper.getMappingContext().getRequiredPersistentEntity(type);
				Object id = ModelUtils.getId(row, entity, name -> mapping.fieldAliasesByTableAlias.get(join.alias).get(name));
				if (id == null) {
					// left join without any match
					if (isCollection)
						field.set(parent, CollectionFactory.createCollection(field.getType(), ModelUtils.getCollectionType(field), 0));
					continue;
				}
				Object instance = resultContext.getEntityCache().getCachedInstance(type, id);
				if (instance == null) {
					instance = mapper.read(join.targetType, createRow(row, entity, join.alias, mapping), null, resultContext);
					resultContext.getEntityCache().setCachedInstance(type, id, instance);
				}
				if (isCollection)
					ModelUtils.addToCollectionField(field, parent, instance);
				else
					field.set(parent, instance);
				fillLinkedEntities(instance, join, row, mapping, resultContext);
			} catch (Exception e) {
				throw new MappingException("Error mapping result for entity " + join.targetType.getName(), e);
			}
		}
	}
	
	
	private static Row createRow(Map<String, Object> data, RelationalPersistentEntity<?> entity, String tableAlias, SelectMapping mapping) {
		RowWrapper r = new RowWrapper();
		Map<String, String> aliases = mapping.fieldAliasesByTableAlias.get(tableAlias);
		for (RelationalPersistentProperty property : entity) {
			String fieldAlias = aliases.get(property.getName());
			r.names.add(property.getColumnName().toString());
			r.data.add(data.get(fieldAlias));
		}
		return r;
	}

	private static class RowWrapper implements Row {
		private List<Object> data = new LinkedList<>();
		private List<String> names = new LinkedList<>();
		
		@SuppressWarnings("unchecked")
		@Override
		public <T> T get(int index, Class<T> type) {
			if (index < 0 || index >= data.size())
				return null;
			return (T) data.get(index);
		}
		
		@Override
		public <T> T get(String name, Class<T> type) {
			return get(names.indexOf(name), type);
		}
		
		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append('{');
			Iterator<Object> itData = data.iterator();
			Iterator<String> itName = names.iterator();
			while (itData.hasNext()) {
				s.append(itName.next()).append('=').append(itData.next());
				if (itData.hasNext())
					s.append(", ");
			}
			return s.toString();
		}
	}
	
	private void endOfRoot() {
		signalLoadedForeignTables(currentRoot, query.from);
	}
	
	@SuppressWarnings("squid:S3011")
	private void signalLoadedForeignTables(Object parent, TableReference parentTable) {
		for (TableReference join : query.joins) {
			if (join.source != parentTable)
				continue;
			try {
				Field field = parent.getClass().getDeclaredField(join.propertyName);
				field.setAccessible(true);
				Object instance = field.get(parent);
				if (field.isAnnotationPresent(ForeignTable.class)) {
					EntityState.get(parent, client).foreignTableLoaded(field, instance);
				}
				boolean isCollection = ModelUtils.isCollection(field);
				if (isCollection)
					for (Object element : ModelUtils.getAsCollection(instance))
						signalLoadedForeignTables(element, join);
				else
					signalLoadedForeignTables(instance, join);
			} catch (Exception e) {
				throw new MappingException("Error mapping result for entity " + join.targetType.getName(), e);
			}
		}
	}
}
