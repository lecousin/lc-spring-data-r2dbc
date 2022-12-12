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

import java.lang.reflect.Field;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.sql.AssignValue;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.Insert;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.SimpleFunction;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.Update;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.util.Assert;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import net.lecousin.reactive.data.relational.mapping.LcEntityWriter;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.model.metadata.EntityInstance;
import net.lecousin.reactive.data.relational.model.metadata.EntityMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyStaticMetadata;
import net.lecousin.reactive.data.relational.query.InsertMultiple;
import net.lecousin.reactive.data.relational.query.SqlQuery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Process requests to save (insert or update) entities in a global operation.
 * 
 * @author Guillaume Le Cousin
 *
 */
class SaveProcessor extends AbstractInstanceProcessor<SaveProcessor.SaveRequest> {

	static class SaveRequest extends AbstractInstanceProcessor.Request {

		<T> SaveRequest(EntityInstance<T> instance) {
			super(instance);
			if (!instance.getState().isLoaded() && instance.getState().isPersisted())
				this.toProcess = false;
		}
		
	}
	
	@Override
	protected <T> SaveRequest createRequest(EntityInstance<T> instance) {
		return new SaveRequest(instance);
	}
	
	@Override
	protected boolean doProcess(Operation op, SaveRequest request) {
		return true;
	}
	
	@Override
	protected void processForeignKey(Operation op, SaveRequest request, PropertyMetadata fkProperty, @Nullable PropertyStaticMetadata foreignTableInfo) {
		Object value = request.entity.getValue(fkProperty);
		Object originalValue = request.entity.getState().getPersistedValue(fkProperty.getName());
		if (!Objects.equals(originalValue, value) && originalValue != null) {
			// link changed, we need to delete/null the previous one
			EntityInstance<?> originalInstance = op.lcClient.getInstance(originalValue);
			// remove the link
			if (foreignTableInfo != null)
				removeForeignTableLink(request, foreignTableInfo, originalInstance);
			if ((foreignTableInfo != null && !foreignTableInfo.getForeignTableAnnotation().optional()) || fkProperty.getForeignKeyAnnotation().cascadeDelete()) {
				// not optional specified on ForeignTable, or cascadeDelete -> this is a delete
				op.addToDelete(originalInstance);
			}
		}
		if (value != null) {
			SaveRequest save = op.addToSave(op.lcClient.getInstance(value));
			if (!save.entity.getState().isPersisted())
				request.dependsOn(save); // if the foreign id is not yet available, we depend on it
		}
	}
	
	private static void removeForeignTableLink(SaveRequest request, PropertyStaticMetadata foreignTableInfo, EntityInstance<?> originalInstance) {
		try {
			if (foreignTableInfo.isCollection()) {
				ModelUtils.removeFromCollectionField(foreignTableInfo.getField(), originalInstance.getEntity(), request.entity.getEntity());
			} else {
				originalInstance.getState().setForeignTableField(originalInstance.getEntity(), foreignTableInfo, null);
			}
		} catch (Exception e) {
			throw new ModelAccessException("Unable to remove link for removed entity", e);
		}
	}
	
	@Override
	protected void processForeignTableField(Operation op, SaveRequest request, PropertyStaticMetadata foreignTableInfo, @Nullable MutableObject<?> foreignFieldValue, PropertyMetadata fkProperty) {
		if (foreignFieldValue == null)
			return; // not loaded -> not saved
		if (foreignTableInfo.isCollection())
			processForeignTableFieldCollection(op, request, foreignTableInfo.getField(), foreignFieldValue, fkProperty);
		else
			processForeignTableFieldSimple(op, request, foreignTableInfo.getField(), foreignFieldValue, fkProperty);
	}
	
	@SuppressWarnings("unchecked")
	private static void processForeignTableFieldCollection(Operation op, SaveRequest request, Field foreignTableField, MutableObject<?> foreignFieldValue, PropertyMetadata fkProperty) {
		Object value = foreignFieldValue.getValue();
		Object originalValue = request.entity.getState().getPersistedValue(foreignTableField.getName());
		if (value == null) {
			if (originalValue == null)
				return; // was already empty
			value = new ArrayList<>(0);
		}
		List<Object> deletedElements = new LinkedList<>();
		if (originalValue != null)
			deletedElements.addAll(ModelUtils.getAsCollection(originalValue));
		deletedElements.removeAll(ModelUtils.getAsCollection(value));

		if (!deletedElements.isEmpty())
			deletedElements(op, deletedElements, fkProperty);
		
		for (Object element : ModelUtils.getAsCollection(value)) {
			op.addToSave(op.lcClient.getInstance(element));
			ModelUtils.setFieldValue(element, fkProperty.getStaticMetadata().getField(), request.entity.getEntity());
		}
	}
	
	private static void deletedElements(Operation op, List<Object> deletedElements, PropertyMetadata fkProperty) {
		ForeignKey fkAnnotation = fkProperty.getForeignKeyAnnotation();
		if (!fkAnnotation.optional() || fkAnnotation.onForeignDeleted().equals(ForeignKey.OnForeignDeleted.DELETE)) {
			// delete
			for (Object element : deletedElements)
				op.addToDelete(op.lcClient.getInstance(element));
		} else {
			// update to null
			for (Object element : deletedElements) {
				EntityInstance<?> instance = op.lcClient.getInstance(element);
				op.addToSave(instance);
				instance.setValue(fkProperty, null);
			}
		}
	}
	
	private static void processForeignTableFieldSimple(Operation op, SaveRequest request, Field foreignTableField, MutableObject<?> foreignFieldValue, PropertyMetadata fkProperty) {
		Object value = foreignFieldValue.getValue();
		Object originalValue = request.entity.getState().getPersistedValue(foreignTableField.getName());
		if (!Objects.equals(originalValue, value) && originalValue != null) {
			// it has been changed, we need to update/delete the previous one
			EntityInstance<?> originalInstance = op.lcClient.getInstance(originalValue);
			ForeignKey fkAnnotation = fkProperty.getForeignKeyAnnotation();
			if (!fkAnnotation.optional() || fkAnnotation.onForeignDeleted().equals(ForeignKey.OnForeignDeleted.DELETE)) {
				// delete
				op.addToDelete(originalInstance);
			} else {
				// update to null
				op.addToSave(originalInstance);
				ModelUtils.setFieldValue(originalValue, fkProperty.getStaticMetadata().getField(), null);
			}
		}
		if (value != null) {
			// save value
			op.addToSave(op.lcClient.getInstance(value));
			ModelUtils.setFieldValue(value, fkProperty.getStaticMetadata().getField(), request.entity.getEntity());
		}
	}
	
	@Override
	protected Mono<Void> doRequests(Operation op, EntityMetadata entityType, List<SaveRequest> requests) {
		List<Mono<Void>> statements = new LinkedList<>();
		boolean multipleInsertSupported = op.lcClient.getSchemaDialect().isMultipleInsertSupported();
		List<SaveRequest> toInsert = new LinkedList<>();
		for (SaveRequest request : requests) {
			if (!request.entity.getState().isPersisted()) {
				if (!multipleInsertSupported)
					statements.add(doInsertSingle(op, request));
				else
					toInsert.add(request);
			} else
				statements.add(doUpdate(op, request));
		}
		doInsert(op, entityType, toInsert, statements);
		return Operation.executeParallel(statements);
	}
	
	private static void doInsert(Operation op, EntityMetadata entityType, List<SaveRequest> requests, List<Mono<Void>> statements) {
		if (requests.isEmpty())
			return;
		List<SaveRequest> remaining = requests;
		do {
			if (remaining.size() == 1) {
				statements.add(doInsertSingle(op, remaining.get(0)));
				return;
			}
			if (remaining.size() <= 1000) {
				statements.add(doInsertMultiple(op, entityType, remaining));
				return;
			}
			statements.add(doInsertMultiple(op, entityType, remaining.subList(0, 1000)));
			remaining = remaining.subList(1000, remaining.size());
		} while (true);
	}
	
	@SuppressWarnings({"java:S1612", "java:S3776"}) // cannot do it
	private static Mono<Void> doInsertMultiple(Operation op, EntityMetadata entityType, List<SaveRequest> requests) {
		return Flux.defer(() -> {
			SqlQuery<InsertMultiple> query = new SqlQuery<>(op.lcClient);
			// table
			Table table = Table.create(entityType.getTableName());
			// columns
			final List<Column> columns = new LinkedList<>();
			final List<PropertyMetadata> generated = new LinkedList<>();
			for (PropertyMetadata property : entityType.getPersistentProperties()) {
				if (property.isGeneratedValue()) {
					GeneratedValue.Strategy strategy = property.getGeneratedValueStrategy();
					if (GeneratedValue.Strategy.SEQUENCE.equals(strategy)) {
						columns.add(Column.create(property.getColumnName(), table));
						generated.add(property);
					} else if (GeneratedValue.Strategy.RANDOM_UUID.equals(strategy) && !op.lcClient.getSchemaDialect().supportsUuidGeneration()) {
						columns.add(Column.create(property.getColumnName(), table));
					} else {
						generated.add(property);
					}
				} else {
					columns.add(Column.create(property.getColumnName(), table));
				}
			}
			
			// values
			List<List<Expression>> rows = new LinkedList<>();
			for (SaveRequest request : requests) {
				Map<SqlIdentifier, Expression> generatedValues = new HashMap<>();
				OutboundRow row = new OutboundRow();
				LcEntityWriter writer = new LcEntityWriter(op.lcClient.getMapper());
				long currentDate = System.currentTimeMillis();
				for (PropertyMetadata property : request.entity.getMetadata().getPersistentProperties()) {
					if (property.isGeneratedValue()) {
						GeneratedValue.Strategy strategy = property.getGeneratedValueStrategy();
						if (GeneratedValue.Strategy.SEQUENCE.equals(strategy)) {
							generatedValues.put(property.getColumnName(), SimpleFunction.create(op.lcClient.getSchemaDialect().sequenceNextValueFunctionName(), Arrays.asList(SQL.literalOf(property.getRequiredGeneratedValueAnnotation().sequence()))));
						} else if (GeneratedValue.Strategy.RANDOM_UUID.equals(strategy) && !op.lcClient.getSchemaDialect().supportsUuidGeneration()) {
							UUID uuid = UUID.randomUUID();
							request.entity.setValue(property, uuid);
							writer.writeProperty(row, property, request.entity.getPropertyAccessor());
						}
					} else { 
						if (property.isVersion()) {
							// Version 1 for an insert
							request.entity.setValue(property, op.lcClient.getMapper().getConversionService().convert(1L, property.getType()));
						} else if (property.isCreatedDate() || property.isLastModifiedDate()) {
							request.entity.setValue(property, getDateValue(currentDate, property.getType()));
						}
						writer.writeProperty(row, property, request.entity.getPropertyAccessor());
					}
				}
				List<Expression> values = new ArrayList<>(columns.size());
				for (Column col : columns) {
					Expression value = generatedValues.get(col.getReferenceName());
					if (value != null)
						values.add(value);
					else {
						Parameter val = row.get(col.getReferenceName());
						if (val.getValue() == null)
							values.add(SQL.nullLiteral());
						else
							values.add(query.marker(val.getValue()));
					}
				}
				rows.add(values);
			}
			
			query.setQuery(new InsertMultiple(table, columns, rows));
			LinkedList<SaveRequest> queue = new LinkedList<>(requests);
			
			return query.execute()
				.filter(statement -> statement.returnGeneratedValues())
				.map((r, meta) -> {
					SaveRequest request = queue.removeFirst();
					mapGeneratedValues(r, meta, request.entity, generated);
					return request.entity.getEntity();
				})
				.all()
				;
		}).then();
	}
	
	@SuppressWarnings({"java:S1612", "java:S3776"}) // cannot do it
	private static Mono<Void> doInsertSingle(Operation op, SaveRequest request) {
		return Mono.fromCallable(() -> {
			SqlQuery<Insert> query = new SqlQuery<>(op.lcClient);
			final List<PropertyMetadata> generated = new LinkedList<>();
			OutboundRow row = new OutboundRow();
			LcEntityWriter writer = new LcEntityWriter(op.lcClient.getMapper());
			long currentDate = System.currentTimeMillis();
			for (PropertyMetadata property : request.entity.getMetadata().getPersistentProperties()) {
				if (property.isGeneratedValue()) {
					if (GeneratedValue.Strategy.RANDOM_UUID.equals(property.getGeneratedValueStrategy()) && !op.lcClient.getSchemaDialect().supportsUuidGeneration()) {
						UUID uuid = UUID.randomUUID();
						request.entity.setValue(property, uuid);
						writer.writeProperty(row, property, request.entity.getPropertyAccessor());
					} else {
						generated.add(property);
					}
				} else { 
					if (property.isVersion()) {
						// Version 1 for an insert
						request.entity.setValue(property, op.lcClient.getMapper().getConversionService().convert(1L, property.getType()));
					} else if (property.isCreatedDate() || property.isLastModifiedDate()) {
						request.entity.setValue(property, getDateValue(currentDate, property.getType()));
					}
					writer.writeProperty(row, property, request.entity.getPropertyAccessor());
				}
			}
			
			query.setQuery(createInsertQuery(query, row, request.entity.getMetadata().getTableName(), generated));
			
			return query.execute()
				.filter(statement -> statement.returnGeneratedValues())
				.map((r, meta) -> {
					mapGeneratedValues(r, meta, request.entity, generated);
					return request.entity.getEntity();
				});
		}).flatMap(RowsFetchSpec::first).then();
	}
	
	private static Insert createInsertQuery(SqlQuery<Insert> query, OutboundRow row, SqlIdentifier tableName, List<PropertyMetadata> generated) {
		Table table = Table.create(tableName);
		List<Column> columns = new ArrayList<>(row.size());
		List<Expression> values = new ArrayList<>(row.size());
		for (PropertyMetadata property : generated) {
			if (GeneratedValue.Strategy.SEQUENCE.equals(property.getGeneratedValueStrategy())) {
				columns.add(Column.create(property.getColumnName(), table));
				values.add(SimpleFunction.create(query.getClient().getSchemaDialect().sequenceNextValueFunctionName(), Arrays.asList(SQL.literalOf(property.getRequiredGeneratedValueAnnotation().sequence()))));
			}
		}
		for (Map.Entry<SqlIdentifier, Parameter> entry : row.entrySet()) {
			columns.add(Column.create(entry.getKey(), table));
			if (entry.getValue().getValue() == null)
				values.add(SQL.nullLiteral());
			else
				values.add(query.marker(entry.getValue().getValue()));
		}
		return Insert.builder().into(table).columns(columns).values(values).build();
	}
	
	private static void mapGeneratedValues(Row row, RowMetadata meta, EntityInstance<?> entity, List<PropertyMetadata> generated) {
		if (!generated.isEmpty()) {
			if (meta.getColumnMetadatas().size() == generated.size()) {
				int index = 0;
				for (PropertyMetadata property : generated)
					entity.setValue(property, entity.getMetadata().getClient().getSchemaDialect().convertFromDataBase(row.get(index++), property.getType()));
			} else {
				IdentifierProcessing idp = entity.getMetadata().getClient().getDialect().getIdentifierProcessing();
				for (PropertyMetadata property : generated)
					entity.setValue(property, entity.getMetadata().getClient().getSchemaDialect().convertFromDataBase(row.get(property.getColumnName().toSql(idp)), property.getType()));
			}
		}
		entity.getState().loaded(entity.getEntity());
	}

	
	private static Mono<Void> doUpdate(Operation op, SaveRequest request) {
		return Mono.fromCallable(() -> createUpdateRequest(op, request))
			.flatMap(updatedRows -> updatedRows != null ? updatedRows.doOnSuccess(nb -> entityUpdated(op, request)).then() : Mono.empty());
	}
	
	private static Mono<Integer> createUpdateRequest(Operation op, SaveRequest request) {
		SqlQuery<Update> query = new SqlQuery<>(op.lcClient);
		Table table = Table.create(request.entity.getMetadata().getTableName());
		OutboundRow row = new OutboundRow();
		LcEntityWriter writer = new LcEntityWriter(op.lcClient.getMapper());
		List<AssignValue> assignments = new LinkedList<>();
		if (!prepareUpdate(request, table, assignments, row, writer, query))
			return null;
		
		for (Map.Entry<SqlIdentifier, Parameter> entry : row.entrySet())
			assignments.add(AssignValue.create(Column.create(entry.getKey(), table), entry.getValue().getValue() != null ? query.marker(entry.getValue().getValue()) : SQL.nullLiteral()));

		Condition criteria = request.entity.getConditionOnId(query);
		
		if (request.entity.getMetadata().hasVersionProperty()) {
			PropertyMetadata property = request.entity.getMetadata().getRequiredVersionProperty();
			Object value = request.entity.getValue(property);
			Assert.notNull(value, "Version must not be null");
			long currentVersion = ((Number)value).longValue();
			criteria = criteria.and(Conditions.isEqual(Column.create(property.getColumnName(), table), query.marker(Long.valueOf(currentVersion))));
		}
		
		query.setQuery(Update.builder().table(table).set(assignments).where(criteria).build());
		Mono<Integer> rowsUpdated = query.execute().fetch().rowsUpdated();
		if (request.entity.getMetadata().hasVersionProperty())
			rowsUpdated = rowsUpdated.flatMap(updatedRows -> {
				if (updatedRows.intValue() == 0)
					return Mono.error(new OptimisticLockingFailureException("Version does not match"));
				return Mono.just(updatedRows);
			});
		return rowsUpdated;
	}
	
	private static boolean prepareUpdate(SaveRequest request, Table table, List<AssignValue> assignments, OutboundRow row, LcEntityWriter writer, SqlQuery<Update> query) {
		boolean hasUpdate = false;
		Map<PropertyMetadata, Object> propertiesToSetIfUpdate = new HashMap<>();
		long currentDate = System.currentTimeMillis();
		for (PropertyMetadata property : request.entity.getMetadata().getPersistentProperties()) {
			if (property.isVersion()) {
				Object value = request.entity.getValue(property);
				Assert.notNull(value, "Version must not be null (property " + property.getName() + " on " + request.entity.getType().getSimpleName() + ")");
				long currentVersion = ((Number)value).longValue();
				assignments.add(AssignValue.create(Column.create(property.getColumnName(), table), query.marker(Long.valueOf(currentVersion + 1))));
			} else if (property.isLastModifiedDate()) {
				propertiesToSetIfUpdate.put(property, getDateValue(currentDate, property.getType()));
			} else if (needsUpdate(request, property)) {
				if (property.isUpdatable()) {
					writer.writeProperty(row, property, request.entity.getPropertyAccessor());
					hasUpdate = true;
				} else {
					request.entity.getState().restorePersistedValue(request.entity.getEntity(), property.getStaticMetadata());
				}
			}
		}
		if (hasUpdate) {
			for (Map.Entry<PropertyMetadata, Object> e : propertiesToSetIfUpdate.entrySet()) {
				request.entity.setValue(e.getKey(), e.getValue());
				writer.writeProperty(row, e.getKey(), request.entity.getPropertyAccessor());
			}
		}
		return hasUpdate;
	}
	
	private static boolean needsUpdate(SaveRequest request, PropertyMetadata property) {
		Object persisted = request.entity.getState().getPersistedValue(property.getName());
		Object actual = request.entity.getValue(property);
		return !Objects.deepEquals(persisted, actual);
	}
	
	private static void entityUpdated(Operation op, SaveRequest request) {
		request.entity.getState().load(request.entity.getEntity());
		if (request.entity.getMetadata().hasVersionProperty()) {
			PropertyMetadata property = request.entity.getMetadata().getRequiredVersionProperty();
			Object version = request.entity.getValue(property);
			Assert.notNull(version, "Version must not be null");
			request.entity.setValue(property, op.lcClient.getMapper().getConversionService().convert(((Number)version).longValue() + 1, property.getType()));
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <T> T getDateValue(long timestamp, Class<T> type) {
		if (type.equals(long.class) || type.equals(Long.class))
			return (T) Long.valueOf(timestamp);
		if (type.isAssignableFrom(java.time.Instant.class))
			return (T) java.time.Instant.ofEpochMilli(timestamp);
		if (type.isAssignableFrom(java.time.LocalDate.class))
			return (T) java.time.LocalDate.ofInstant(java.time.Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
		if (type.isAssignableFrom(java.time.LocalTime.class))
			return (T) java.time.LocalTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
		if (type.isAssignableFrom(java.time.OffsetTime.class))
			return (T) java.time.OffsetTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
		if (type.isAssignableFrom(java.time.LocalDateTime.class))
			return (T) java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
		if (type.isAssignableFrom(java.time.ZonedDateTime.class))
			return (T) java.time.ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
		return null;
	}
	
}
