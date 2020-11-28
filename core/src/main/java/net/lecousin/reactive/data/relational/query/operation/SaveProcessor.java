package net.lecousin.reactive.data.relational.query.operation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.annotation.Version;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.AssignValue;
import org.springframework.data.relational.core.sql.Assignment;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Insert;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.Update;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.r2dbc.core.RowsFetchSpec;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.mapping.LcEntityWriter;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.query.SqlQuery;
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
			SqlQuery<Insert> query = new SqlQuery<>(op.lcClient);
			Table table = Table.create(request.entityType.getTableName());
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
				for (Map.Entry<SqlIdentifier, Parameter> entry : row.entrySet())
					debug.append(entry.getKey()).append('=').append(entry.getValue().getValue());
				LcReactiveDataRelationalClient.logger.debug(debug.toString());
			}

			List<Column> columns = new ArrayList<>(row.size());
			List<Expression> values = new ArrayList<>(row.size());
			for (Map.Entry<SqlIdentifier, Parameter> entry : row.entrySet()) {
				columns.add(Column.create(entry.getKey(), table));
				if (entry.getValue().getValue() == null)
					values.add(SQL.nullLiteral());
				else
					values.add(query.marker(entry.getValue().getValue()));
			}
			query.setQuery(Insert.builder().into(table).columns(columns).values(values).build());
			
			return query.execute().map((r, meta) -> {
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
			SqlQuery<Update> query = new SqlQuery<>(op.lcClient);
			Table table = Table.create(request.entityType.getTableName());
			OutboundRow row = new OutboundRow();
			LcEntityWriter writer = new LcEntityWriter(op.lcClient.getMapper());
			List<Assignment> assignments = new LinkedList<>();
			Condition criteria = null;
			for (RelationalPersistentProperty property : request.entityType) {
				if (property.isAnnotationPresent(Version.class)) {
					Object value = request.accessor.getProperty(property);
					long currentVersion = ((Number)value).longValue();
					Condition c = Conditions.isEqual(Column.create(property.getColumnName(), table), query.marker(Long.valueOf(currentVersion)));
					criteria = criteria != null ? criteria.and(c) : c;
					assignments.add(AssignValue.create(Column.create(property.getColumnName(), table), query.marker(Long.valueOf(currentVersion + 1))));
				} else if (!property.isIdProperty() && request.state.isFieldModified(property.getField().getName()) && property.isWritable()) {
					writer.writeProperty(row, property, request.accessor);
				}
			}
			if (row.isEmpty())
				return null;
			for (Map.Entry<SqlIdentifier, Parameter> entry : row.entrySet())
				assignments.add(AssignValue.create(Column.create(entry.getKey(), table), query.marker(entry.getValue().getValue())));

			RelationalPersistentProperty idProperty = request.entityType.getRequiredIdProperty();
			Object id = request.accessor.getProperty(idProperty);
			Condition c = Conditions.isEqual(Column.create(idProperty.getColumnName(), table), query.marker(id));
			criteria = criteria != null ? criteria.and(c) : c;
			if (LcReactiveDataRelationalClient.logger.isDebugEnabled()) {
				StringBuilder debug = new StringBuilder("Update ").append(request.entityType.getName());
				for (Assignment entry : assignments)
					debug.append(entry).append(',');
				debug.append(" WHERE ").append(idProperty.getName()).append('=').append(id);
				LcReactiveDataRelationalClient.logger.debug(debug.toString());
			}
			query.setQuery(Update.builder().table(table).set(assignments).where(criteria).build());
			return query.execute()
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
