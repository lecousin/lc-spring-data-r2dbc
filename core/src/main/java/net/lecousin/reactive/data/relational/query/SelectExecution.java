package net.lecousin.reactive.data.relational.query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.data.mapping.MappingException;
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
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.mapping.LcEntityReader;
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
			.flatMapMany(needsPreSelect -> needsPreSelect.booleanValue() ? executeWithPreSelect() : executeWithoutPreSelect());
	}
	
	private boolean needsPreSelectIds() {
		// first step is to ensure we wave the target type for all joins
		query.setJoinsTargetType(client.getMapper());
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
	
	private boolean needsTableForPreSelect(TableReference table) {
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
				String idPropertyName = mapping.entitiesByAlias.get(query.from.alias).getIdProperty().getName();
				Flux<Map<String, Object>> fromDb = buildFinalSql(mapping, Criteria.property(query.from.alias, idPropertyName).in(ids), false).execute().fetch().all();
				return Flux.create(sink ->
					fromDb.doOnComplete(() -> handleRow(null, sink, mapping))
						.subscribe(row -> handleRow(row, sink, mapping))
				);
			});
	}
	
	private Flux<T> executeWithoutPreSelect() {
		SelectMapping mapping = buildSelectMapping();
		Flux<Map<String, Object>> fromDb = buildFinalSql(mapping, query.where, true).execute().fetch().all();
		return Flux.create(sink ->
			fromDb.doOnComplete(() -> handleRow(null, sink, mapping))
				.subscribe(row -> handleRow(row, sink, mapping))
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
	
	private SqlQuery<Select> buildFinalSql(SelectMapping mapping, Criteria criteria, boolean applyLimit) {
		RelationalPersistentEntity<?> entity = client.getMappingContext().getRequiredPersistentEntity(query.from.targetType);
		
		List<Column> selectFields = new ArrayList<>(mapping.fields.size());
		for (SelectField field : mapping.fields)
			selectFields.add(field.toSql());
		BuildSelect select = Select.builder().select(selectFields).from(mapping.tableByAlias.get(query.from.alias));
		if (applyLimit && query.limit > 0) {
			select = ((SelectFromAndJoin)select).limitOffset(query.limit, query.offset);
		}
		
		for (TableReference join : query.joins) {
			select = join(select, join, mapping);
		}

		SqlQuery<Select> q = new SqlQuery<>(client);
		if (criteria != null) {
			select = ((SelectWhere)select).where(criteria.accept(new CriteriaSqlBuilder(mapping.entitiesByAlias, mapping.tableByAlias, q)));
		}
		if (entity.hasIdProperty()) {
			select = ((SelectOrdered)select).orderBy(Column.create(mapping.fieldAliasesByTableAlias.get(query.from.alias).get(entity.getRequiredIdProperty().getName()), mapping.tableByAlias.get(query.from.alias)));
		}
		
		q.setQuery(select.build());
		return q;
	}

	
	private SqlQuery<Select> buildDistinctRootIdSql(SelectMapping mapping) {
		RelationalPersistentEntity<?> entity = client.getMappingContext().getRequiredPersistentEntity(query.from.targetType);
		
		BuildSelect select = Select.builder().select(Column.create(entity.getIdColumn(), mapping.tableByAlias.get(query.from.alias))).distinct().from(mapping.tableByAlias.get(query.from.alias));
		if (query.limit > 0) {
			select = ((SelectFromAndJoin)select).limitOffset(query.limit, query.offset);
		}
		
		for (TableReference join : query.joins) {
			if (!needsTableForPreSelect(join))
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
	
	private BuildSelect join(BuildSelect select, TableReference join, SelectMapping mapping) {
		RelationalPersistentEntity<?> sourceEntity = client.getMappingContext().getRequiredPersistentEntity(join.source.targetType); 
		RelationalPersistentEntity<?> targetEntity = client.getMappingContext().getRequiredPersistentEntity(join.targetType); 
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
		return select;
	}
	
	private T currentRoot = null;
	private Object currentRootId = null;
	
	@SuppressWarnings("unchecked")
	private void handleRow(Map<String, Object> row, FluxSink<T> sink, SelectMapping mapping) {
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
		RelationalPersistentEntity<?> rootEntity = client.getMappingContext().getRequiredPersistentEntity(query.from.targetType);
		PropertiesSource source = new PropertiesSourceMap(row, mapping.fieldAliasesByTableAlias.get(query.from.alias));
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
		fillLinkedEntities(currentRoot, EntityState.get(currentRoot, client, rootEntity), query.from, row, mapping, reader);
	}
	
	private void fillLinkedEntities(Object parent, EntityState parentState, TableReference parentTable, Map<String, Object> row, SelectMapping mapping, LcEntityReader reader) {
		for (TableReference join : query.joins) {
			if (join.source != parentTable)
				continue;
			try {
				fillLinkedEntity(join, parent, parentState, row, mapping, reader);
			} catch (Exception e) {
				throw new MappingException("Error mapping result for entity " + join.targetType.getName(), e);
			}
		}
	}
	
	@SuppressWarnings({"java:S3011", "unchecked"}) // access directly to field
	private <J> void fillLinkedEntity(TableReference join, Object parent, EntityState parentState, Map<String, Object> row, SelectMapping mapping, LcEntityReader reader) throws ReflectiveOperationException {
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
			if (ModelUtils.isForeignTableField(field))
				parentState.setForeignTableField(parent, field, instance, true);
			else
				parentState.setPersistedField(parent, field, instance, true);
		}
		fillLinkedEntities(instance, EntityState.get(instance, client, entity), join, row, mapping, reader);
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
				if (instance != null) {
					boolean isCollection = ModelUtils.isCollection(field);
					if (isCollection)
						for (Object element : ModelUtils.getAsCollection(instance))
							signalLoadedForeignTables(element, join);
					else
						signalLoadedForeignTables(instance, join);
				}
			} catch (Exception e) {
				throw new MappingException("Error mapping result for entity " + join.targetType.getName(), e);
			}
		}
	}
}
