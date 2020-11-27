package net.lecousin.reactive.data.relational.query.operation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.annotation.Version;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.r2dbc.core.DatabaseClient.GenericInsertSpec;
import org.springframework.data.r2dbc.core.RowsFetchSpec;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.SettableValue;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.mapping.LcEntityWriter;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import reactor.core.publisher.Mono;

class SaveProcessor extends AbstractProcessor<SaveProcessor.SaveRequest> {

	static class SaveRequest extends AbstractProcessor.Request {

		<T> SaveRequest(RelationalPersistentEntity<T> entityType, T instance, EntityState state, PersistentPropertyAccessor<T> accessor) {
			super(entityType, instance, state, accessor);
		}
		
	}
	
	@Override
	protected <T> SaveRequest createRequest(T instance, EntityState state, RelationalPersistentEntity<T> entity, PersistentPropertyAccessor<T> accessor) {
		return new SaveRequest(entity, instance, state, accessor);
	}
	
	@Override
	protected boolean checkRequest(Operation op, SaveRequest request) {
		return true;
	}
	
	@Override
	protected void processForeignKey(
		Operation op, SaveRequest request,
		RelationalPersistentProperty fkProperty, ForeignKey fkAnnotation,
		@Nullable Field foreignTableField, @Nullable ForeignTable foreignTableAnnotation
	) {
		Object value = request.accessor.getProperty(fkProperty);
		Object originalValue = request.state.getPersistedValue(fkProperty.getName());
		if (!Objects.equals(originalValue, value) && originalValue != null) {
			// link changed, we need to delete/null the previous one
			// remove the link
			if (foreignTableAnnotation != null) {
				try {
					if (ModelUtils.isCollection(foreignTableField)) {
						ModelUtils.removeFromCollectionField(foreignTableField, originalValue, request.instance);
					} else {
						EntityState foreignState = EntityState.get(originalValue, op.lcClient);
						foreignState.setForeignTableField(originalValue, foreignTableField, null, false);
					}
				} catch (Exception e) {
					throw new ModelAccessException("Unable to remove link for removed entity", e);
				}
			}
			if ((foreignTableAnnotation != null && !foreignTableAnnotation.optional()) || fkAnnotation.cascadeDelete()) {
				// not optional specified on ForeignTable, or cascadeDelete -> this is a delete
				op.addToDelete(originalValue, null, null, null);
			} else {
				op.addToSave(originalValue, null, null, null);
			}
		}
		if (value != null) {
			SaveRequest save = op.addToSave(value, null, null, null);
			if (!save.state.isPersisted())
				request.dependsOn(save); // if the foreign id is not yet available, we depend on it
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected <T> void processForeignTableField(
		Operation op, SaveRequest request,
		Field foreignTableField, ForeignTable foreignTableAnnotation, MutableObject<?> foreignFieldValue, boolean isCollection,
		RelationalPersistentEntity<T> foreignEntity, RelationalPersistentProperty fkProperty, ForeignKey fkAnnotation
	) {
		if (foreignFieldValue == null)
			return; // not loaded -> not saved
		Object value = foreignFieldValue.getValue();
		if (ModelUtils.isCollection(foreignTableField)) {
			Object originalValue = request.state.getPersistedValue(foreignTableField.getName());
			if (value == null) {
				if (originalValue == null)
					return; // was already empty
				value = new ArrayList<>(0);
			}
			List<Object> deletedElements = new LinkedList<>();
			if (originalValue != null)
				deletedElements.addAll(ModelUtils.getAsCollection(originalValue));
			for (Object element : ModelUtils.getAsCollection(value)) {
				deletedElements.remove(element);
			}
			if (!deletedElements.isEmpty()) {
				if (!fkAnnotation.optional() || fkAnnotation.onForeignDeleted().equals(ForeignKey.OnForeignDeleted.DELETE)) {
					// delete
					for (Object element : deletedElements)
						op.addToDelete((T) element, foreignEntity, null, null);
				} else {
					// update to null
					for (Object element : deletedElements) {
						SaveRequest save = op.addToSave((T) element, foreignEntity, null, null);
						save.state.setPersistedField(element, fkProperty.getField(), null, false);
					}
				}
			}
			for (Object element : ModelUtils.getAsCollection(value)) {
				SaveRequest save = op.addToSave((T) element, foreignEntity, null, null);
				save.state.setPersistedField(element, fkProperty.getField(), request.instance, false);
			}
		} else {
			T originalValue = (T) request.state.getPersistedValue(foreignTableField.getName());
			if (!Objects.equals(originalValue, value) && originalValue != null) {
				// it has been changed, we need to update/delete the previous one
				if (!fkAnnotation.optional() || fkAnnotation.onForeignDeleted().equals(ForeignKey.OnForeignDeleted.DELETE)) {
					// delete
					op.addToDelete(originalValue, foreignEntity, null, null);
				} else {
					// update to null
					SaveRequest save = op.addToSave(originalValue, foreignEntity, null, null);
					save.state.setPersistedField(originalValue, fkProperty.getField(), null, false);
				}
			}
			if (value != null) {
				// save value
				SaveRequest save = op.addToSave((T) value, foreignEntity, null, null);
				save.state.setPersistedField(value, fkProperty.getField(), request.instance, false);
			}
		}
	}
	
	@Override
	protected Mono<Void> doRequests(Operation op, RelationalPersistentEntity<?> entityType, List<SaveRequest> requests) {
		List<Mono<?>> statements = new LinkedList<>();
		for (SaveRequest request : requests) {
			if (!request.state.isPersisted())
				statements.add(doInsert(op, request));
			else
				statements.add(doUpdate(op, request));
		}
		return Mono.when(statements);
	}
	
	private static Mono<Object> doInsert(Operation op, SaveRequest request) {
		return Mono.fromCallable(() -> {
			GenericInsertSpec<Map<String, Object>> insert = op.lcClient.getSpringClient().insert().into(request.entityType.getTableName());
			final List<RelationalPersistentProperty> generated = new LinkedList<>();
			OutboundRow row = new OutboundRow();
			LcEntityWriter writer = new LcEntityWriter(op.lcClient.getMapper());
			for (RelationalPersistentProperty property : request.entityType) {
				if (property.isAnnotationPresent(GeneratedValue.class)) {
					generated.add(property);
				} else if (property.isWritable()) { 
					if (property.isAnnotationPresent(Version.class)) {
						// Version 1 for an insert
						request.accessor.setProperty(property, 1L);
					}
					writer.writeProperty(row, property, request.accessor);
				}
			}
			
			if (LcReactiveDataRelationalClient.logger.isDebugEnabled()) {
				StringBuilder debug = new StringBuilder("Insert into ").append(request.entityType.getName()).append(": ");
				for (Map.Entry<SqlIdentifier, SettableValue> entry : row.entrySet())
					debug.append(entry.getKey()).append('=').append(entry.getValue().getValue());
				LcReactiveDataRelationalClient.logger.debug(debug.toString());
			}

			for (Map.Entry<SqlIdentifier, SettableValue> entry : row.entrySet())
				if (entry.getValue().getValue() == null)
					insert = insert.nullValue(entry.getKey(), entry.getValue().getType());
				else
					insert = insert.value(entry.getKey(), entry.getValue().getValue());
			
			return insert.map((r, meta) -> {
				int index = 0;
				for (RelationalPersistentProperty property : generated)
					request.accessor.setProperty(property, r.get(index++));
				request.state.loaded(request.instance);
				return request.instance;
			});
		}).flatMap(RowsFetchSpec::first);
	}
	
	private static Mono<Object> doUpdate(Operation op, SaveRequest request) {
		return Mono.fromCallable(() -> {
			OutboundRow row = new OutboundRow();
			LcEntityWriter writer = new LcEntityWriter(op.lcClient.getMapper());
			Map<SqlIdentifier, Object> assignments = new HashMap<>();
			Criteria criteria = Criteria.empty();
			for (RelationalPersistentProperty property : request.entityType) {
				if (property.isAnnotationPresent(Version.class)) {
					Object value = request.accessor.getProperty(property);
					long currentVersion = ((Number)value).longValue();
					criteria = criteria.and(Criteria.where(op.lcClient.getDataAccess().toSql(property.getColumnName())).is(Long.valueOf(currentVersion)));
					assignments.put(property.getColumnName(), Long.valueOf(currentVersion + 1));
				} else if (!property.isIdProperty() && request.state.isFieldModified(property.getField().getName()) && property.isWritable()) {
					writer.writeProperty(row, property, request.accessor);
				}
			}
			if (row.isEmpty())
				return null;
			for (Map.Entry<SqlIdentifier, SettableValue> entry : row.entrySet())
				assignments.put(entry.getKey(), entry.getValue().getValue());
			RelationalPersistentProperty idProperty = request.entityType.getRequiredIdProperty();
			Object id = request.accessor.getProperty(idProperty);
			criteria = criteria.and(Criteria.where(op.lcClient.getDataAccess().toSql(idProperty.getColumnName())).is(id));
			if (LcReactiveDataRelationalClient.logger.isDebugEnabled()) {
				StringBuilder debug = new StringBuilder("Update ").append(request.entityType.getName());
				for (Map.Entry<SqlIdentifier, Object> entry : assignments.entrySet())
					debug.append(entry.getKey()).append('=').append(entry.getValue());
				debug.append(" WHERE ").append(idProperty.getName()).append('=').append(id);
				LcReactiveDataRelationalClient.logger.debug(debug.toString());
			}
			return op.lcClient.getSpringClient()
				.update().table(request.entityType.getTableName())
				.using(Update.from(assignments))
				.matching(criteria)
				.fetch().rowsUpdated()
				.flatMap(updatedRows -> {
					if (updatedRows.intValue() == 0)
						return Mono.error(new OptimisticLockingFailureException("Version does not match"));
					return Mono.just(updatedRows);
				});
		}).flatMap(updatedRows -> updatedRows != null ? updatedRows.thenReturn(request.instance).doOnSuccess(e -> entityUpdated(request)) : Mono.just(request.instance));
	}
	
	private static void entityUpdated(SaveRequest request) {
		request.state.load(request.instance);
		for (RelationalPersistentProperty property : request.entityType) {
			if (property.isAnnotationPresent(Version.class)) {
				request.accessor.setProperty(property, ((Long)request.accessor.getProperty(property)) + 1);
			}
		}
	}
	
}
