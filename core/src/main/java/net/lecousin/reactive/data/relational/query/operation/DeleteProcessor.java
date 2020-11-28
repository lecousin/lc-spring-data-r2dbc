package net.lecousin.reactive.data.relational.query.operation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Delete;
import org.springframework.data.relational.core.sql.StatementBuilder;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.query.SqlQuery;
import reactor.core.publisher.Mono;

class DeleteProcessor extends AbstractProcessor<DeleteProcessor.DeleteRequest> {

	/** Leaf entities that can be deleted without the need to load them. */
	private Map<RelationalPersistentEntity<?>, List<Pair<RelationalPersistentProperty, Object>>> toDeleteWithoutLoading = new HashMap<>();
	
	static class DeleteRequest extends AbstractProcessor.Request {
		
		private Map<String, Object> savedForeignKeys = new HashMap<>();

		<T> DeleteRequest(RelationalPersistentEntity<T> entityType, T instance, EntityState state, PersistentPropertyAccessor<T> accessor) {
			super(entityType, instance, state, accessor);
		}
		
		private void saveForeignKeyValue(String foreignKey, Object value) {
			savedForeignKeys.put(foreignKey, value);
		}
		
		private Object getSavedForeignKeyValue(String foreignKey) {
			return savedForeignKeys.get(foreignKey);
		}
		
	}
	
	@Override
	protected <T> DeleteRequest createRequest(T instance, EntityState state, RelationalPersistentEntity<T> entity, PersistentPropertyAccessor<T> accessor) {
		return new DeleteRequest(entity, instance, state, accessor);
	}
	
	@Override
	protected boolean checkRequest(Operation op, DeleteRequest request) {
		return request.state.isPersisted();
	}

	@Override
	@SuppressWarnings("java:S3011")
	protected void processForeignKey(
		Operation op, DeleteRequest request,
		RelationalPersistentProperty fkProperty, ForeignKey fkAnnotation,
		@Nullable Field foreignTableField, @Nullable ForeignTable foreignTableAnnotation
	) {
		if (!request.entityType.hasIdProperty()) {
			// no id, the delete will be by values, but we need to keep foreign key ids because they will be set to null before
			Object foreignInstance = request.accessor.getProperty(fkProperty);
			if (foreignInstance != null) {
				RelationalPersistentEntity<?> fe = op.lcClient.getMappingContext().getRequiredPersistentEntity(foreignInstance.getClass());
				foreignInstance = fe.getPropertyAccessor(foreignInstance).getProperty(fe.getRequiredIdProperty());
			}
			request.saveForeignKeyValue(fkProperty.getName(), foreignInstance);
		}
		if (foreignTableAnnotation != null && foreignTableField != null) {
			if (ModelUtils.isCollection(foreignTableField)) {
				// remove from collection if loaded
				removeFromForeignTableCollection(request, fkProperty, foreignTableField);
				return;
			}
			
			if (foreignTableAnnotation.optional() && !fkAnnotation.cascadeDelete()) {
				// set to null if loaded
				if (request.state.isLoaded() && !request.state.isFieldModified(fkProperty.getName())) {
					Object foreignInstance = request.accessor.getProperty(fkProperty);
					if (foreignInstance != null) {
						try {
							foreignTableField.set(foreignInstance, null);
						} catch (Exception e) {
							throw new ModelAccessException("Cannot set foreign table field", e);
						}
					}
				}
				return;
			}
		} else if (!fkAnnotation.cascadeDelete()) {
			// no ForeignTable, and no cascade => do nothing
			return;
		}
		
		// delete
		if (request.state.isLoaded()) {
			deleteForeignKeyInstance(op, request, request.state.getPersistedValue(fkProperty.getName()));
			return;
		}

		op.loader.load(request.entityType, request.instance, loaded -> deleteForeignKeyInstance(op, request, request.accessor.getProperty(fkProperty)));
	}
	
	private static void removeFromForeignTableCollection(DeleteRequest request, RelationalPersistentProperty fkProperty, Field foreignTableField) {
		if (request.state.isLoaded() && !request.state.isFieldModified(fkProperty.getName())) {
			Object foreignInstance = request.accessor.getProperty(fkProperty);
			if (foreignInstance != null) {
				try {
					ModelUtils.removeFromCollectionField(foreignTableField, foreignInstance, request.instance);
				} catch (Exception e) {
					throw new ModelAccessException("Cannot remove instance from collection field", e);
				}
			}
		}
	}
	
	private void deleteForeignKeyInstance(Operation op, DeleteRequest request, Object foreignInstance) {
		if (foreignInstance == null)
			return;
		DeleteRequest deleteForeign = addToProcess(op, foreignInstance, null, null, null);
		request.dependsOn(deleteForeign);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected <T> void processForeignTableField(
		Operation op, DeleteRequest request,
		Field foreignTableField, ForeignTable foreignTableAnnotation, MutableObject<?> foreignFieldValue, boolean isCollection,
		RelationalPersistentEntity<T> foreignEntity, RelationalPersistentProperty fkProperty, ForeignKey fkAnnotation
	) {
		if (fkAnnotation.optional() && fkAnnotation.onForeignDeleted().equals(ForeignKey.OnForeignDeleted.SET_TO_NULL)) {
			// update to null
			Object instId = ModelUtils.getRequiredId(request.instance, request.entityType, request.accessor);
			op.updater.update(foreignEntity, fkProperty, instId, null);
			if (foreignFieldValue != null) {
				Object foreignInstance = foreignFieldValue.getValue();
				if (foreignInstance != null) {
					EntityState.get(foreignInstance, op.lcClient, foreignEntity).setPersistedField(foreignInstance, fkProperty.getField(), null, true);
				}
			}
			return;
		}
		
		//delete
		if (foreignFieldValue != null && !request.state.isFieldModified(foreignTableField.getName())) {
			// foreign loaded
			Object foreignInstance = foreignFieldValue.getValue();
			if (foreignInstance == null)
				return; // no link
			if (ModelUtils.isCollection(foreignTableField)) {
				for (Object o : ModelUtils.getAsCollection(foreignFieldValue.getValue()))
					addToProcess(op, (T) o, foreignEntity, null, null);
			} else {
				addToProcess(op, (T) foreignFieldValue.getValue(), foreignEntity, null, null);
			}
		} else {
			// foreign not loaded
			Object instId = ModelUtils.getRequiredId(request.instance, request.entityType, request.accessor);
			if (!hasOtherLinks(op, foreignEntity.getType(), foreignTableAnnotation.joinKey())) {
				// can do delete where fk in (ids)
				deleteWithoutLoading(foreignEntity, Pair.of(fkProperty, instId));
			} else {
				// need to retrieve the entity from database, then process them to be deleted
				op.loader.retrieve(foreignEntity, fkProperty, instId, loaded -> addToProcess(op, (T) loaded, foreignEntity, null, null));
			}
		}
	}

	
	private void deleteWithoutLoading(RelationalPersistentEntity<?> entity, Pair<RelationalPersistentProperty, Object> criteria) {
		List<Pair<RelationalPersistentProperty, Object>> list = toDeleteWithoutLoading.computeIfAbsent(entity, e -> new LinkedList<>());
		list.add(criteria);
	}
	
	private static boolean hasOtherLinks(Operation op, Class<?> entityType, String otherThanField) {
		for (Pair<Field, ForeignTable> p : ModelUtils.getForeignTables(entityType)) {
			if (!p.getFirst().getName().equals(otherThanField))
				return true;
		}
		RelationalPersistentEntity<?> entity = op.lcClient.getMappingContext().getRequiredPersistentEntity(entityType);
		for (RelationalPersistentProperty prop : entity) {
			if (!prop.getName().equals(otherThanField) && prop.isAnnotationPresent(ForeignKey.class))
				return true;
		}
		return false;
	}
	
	@Override
	protected Mono<Void> doOperations(Operation op) {
		Mono<Void> executeRequests = super.doOperations(op);
		Mono<Void> deleteWithoutLoading = doDeleteWithoutLoading(op);
		if (executeRequests != null) {
			if (deleteWithoutLoading != null)
				return Mono.when(executeRequests, deleteWithoutLoading);
			return executeRequests;
		}
		return deleteWithoutLoading;
	}
	
	@Override
	protected Mono<Void> doRequests(Operation op, RelationalPersistentEntity<?> entityType, List<DeleteRequest> requests) {
		SqlQuery<Delete> delete = new SqlQuery<>(op.lcClient);
		Table table = Table.create(entityType.getTableName());
		Condition criteria = entityType.hasIdProperty() ? createCriteriaOnIds(entityType, requests, delete, table) : createCriteriaOnProperties(entityType, requests, delete, table);
		if (LcReactiveDataRelationalClient.logger.isDebugEnabled())
			LcReactiveDataRelationalClient.logger.debug("Delete " + entityType.getType().getName() + " where " + criteria);
		delete.setQuery(
			StatementBuilder.delete()
			.from(table)
			.where(criteria)
			.build()
		);
		return delete.execute()
			.then()
			.doOnSuccess(v -> op.toCall(() -> deleteDone(entityType, requests)));
	}
	
	private static Condition createCriteriaOnIds(RelationalPersistentEntity<?> entityType, List<DeleteRequest> requests, SqlQuery<Delete> query, Table table) {
		List<Object> ids = new ArrayList<>(requests.size());
		for (DeleteRequest request : requests) {
			Object id = entityType.getPropertyAccessor(request.instance).getProperty(entityType.getRequiredIdProperty());
			ids.add(id);
		}
		return Conditions.in(Column.create(entityType.getRequiredIdProperty().getColumnName(), table), query.marker(ids));
	}
	
	private static Condition createCriteriaOnProperties(RelationalPersistentEntity<?> entityType, List<DeleteRequest> requests, SqlQuery<Delete> query, Table table) {
		Condition criteria = null;
		Iterator<DeleteRequest> it = requests.iterator();
		do {
			DeleteRequest request = it.next();
			Condition c = null;
			Iterator<RelationalPersistentProperty> itProperty = entityType.iterator();
			do {
				RelationalPersistentProperty property = itProperty.next();
				Condition propertyCondition;
				Object value = request.accessor.getProperty(property);
				if (value == null) {
					propertyCondition = Conditions.isNull(Column.create(property.getColumnName(), table));
				} else {
					if (property.isAnnotationPresent(ForeignKey.class)) {
						// get the id instead of the entity
						value = request.getSavedForeignKeyValue(property.getName());
					}
					propertyCondition = Conditions.isEqual(Column.create(property.getColumnName(), table), query.marker(value));
				}
				c = c != null ? c.and(propertyCondition) : propertyCondition;
			} while (itProperty.hasNext());
			criteria = criteria != null ? criteria.or(c) : c;
		} while (it.hasNext());
		return criteria;
	}
	
	private static void deleteDone(RelationalPersistentEntity<?> entityType, List<DeleteRequest> done) {
		for (DeleteRequest request : done) {
			// change the state of the entity instance
			request.state.deleted();
			// set id to null
			RelationalPersistentProperty idProperty = entityType.getIdProperty();
			if (idProperty != null && !idProperty.getType().isPrimitive())
				entityType.getPropertyAccessor(request.instance).setProperty(idProperty, null);
		}
	}

	private Mono<Void> doDeleteWithoutLoading(Operation op) {
		List<Mono<Void>> calls = new LinkedList<>();
		Map<RelationalPersistentEntity<?>, List<Pair<RelationalPersistentProperty, Object>>> map = toDeleteWithoutLoading;
		toDeleteWithoutLoading = new HashMap<>();
		for (Map.Entry<RelationalPersistentEntity<?>, List<Pair<RelationalPersistentProperty, Object>>> entity : map.entrySet()) {
			if (LcReactiveDataRelationalClient.logger.isDebugEnabled())
				LcReactiveDataRelationalClient.logger.debug("Delete " + entity.getKey().getType().getName() + " where " + entity.getValue());
			SqlQuery<Delete> query = new SqlQuery<>(op.lcClient);
			Table table = Table.create(entity.getKey().getTableName());
			Iterator<Pair<RelationalPersistentProperty, Object>> it = entity.getValue().iterator();
			Condition condition = null;
			do {
				Pair<RelationalPersistentProperty, Object> p = it.next();
				Condition c = Conditions.isEqual(Column.create(p.getFirst().getColumnName(), table), query.marker(p.getSecond()));
				condition = condition != null ? condition.or(c) : c;
			} while (it.hasNext());
			query.setQuery(Delete.builder().from(table).where(condition).build());
			calls.add(query.execute().then());
		}
		if (calls.isEmpty())
			return null;
		return Mono.when(calls);
	}
	
}
