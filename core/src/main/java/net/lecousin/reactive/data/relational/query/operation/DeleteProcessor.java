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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Delete;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.StatementBuilder;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.model.metadata.EntityInstance;
import net.lecousin.reactive.data.relational.model.metadata.EntityMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyStaticMetadata;
import net.lecousin.reactive.data.relational.query.SqlQuery;
import reactor.core.publisher.Mono;

/**
 * Process deletes in a global operation.
 * 
 * @author Guillaume Le Cousin
 *
 */
class DeleteProcessor extends AbstractInstanceProcessor<DeleteProcessor.DeleteRequest> {

	static class DeleteRequest extends AbstractInstanceProcessor.Request {
		
		private Map<String, Object> savedForeignKeys = new HashMap<>();

		<T> DeleteRequest(EntityInstance<T> instance) {
			super(instance);
		}
		
		private void saveForeignKeyValue(String foreignKey, Object value) {
			savedForeignKeys.put(foreignKey, value);
		}
		
		private Object getSavedForeignKeyValue(String foreignKey) {
			return savedForeignKeys.get(foreignKey);
		}
		
	}
	
	@Override
	protected <T> DeleteRequest createRequest(EntityInstance<T> instance) {
		return new DeleteRequest(instance);
	}
	
	@Override
	protected boolean doProcess(Operation op, DeleteRequest request) {
		if (!request.entity.getState().isPersisted())
			return false;
		EntityMetadata type = request.entity.getMetadata();
		// check entities having a foreign key, but where we don't have a foreign table link
		for (EntityMetadata entity : op.lcClient.getEntities()) {
			if (entity.equals(type))
				continue;
			
			for (PropertyMetadata fkProperty : entity.getForeignKeys()) {
				if (!fkProperty.getType().equals(type.getType()))
					continue;
				
				PropertyStaticMetadata ft = type.getStaticMetadata().getForeignTableForJoinKey(fkProperty.getName(), entity.getType());
				if (ft == null)
					processForeignTableField(op, request, ft, null, fkProperty);
			}
		}
		return true;
	}
	
	@Override
	@SuppressWarnings({"java:S3011", "java:S3776"})
	protected void processForeignKey(Operation op, DeleteRequest request, PropertyMetadata fkProperty, @Nullable PropertyStaticMetadata foreignTableInfo) {
		if (!request.entity.getMetadata().hasIdProperty()) {
			// no id, the delete will be by values, but we need to keep foreign key ids because they will be set to null before
			Object foreignInstance = request.entity.getValue(fkProperty);
			if (foreignInstance != null) {
				// replace the instance by its primary key
				foreignInstance = request.entity.getForeignKeyValue(fkProperty, foreignInstance);
			}
			request.saveForeignKeyValue(fkProperty.getName(), foreignInstance);
		}
		if (foreignTableInfo != null) {
			if (foreignTableInfo.isCollection()) {
				// remove from collection if loaded
				removeFromForeignTableCollection(request, fkProperty, foreignTableInfo);
				return;
			}
			
			if (foreignTableInfo.getForeignTableAnnotation().optional() && !fkProperty.getForeignKeyAnnotation().cascadeDelete()) {
				// set to null if loaded
				if (request.entity.getState().isLoaded()) {
					Object foreignInstance = request.entity.getValue(fkProperty);
					if (foreignInstance != null && request.entity.isPersistedForeignKey(fkProperty, foreignInstance))
						ModelUtils.setFieldValue(foreignInstance, foreignTableInfo.getField(), null);
				}
				return;
			}
		} else if (!fkProperty.getForeignKeyAnnotation().cascadeDelete()) {
			// no ForeignTable, and no cascade => do nothing
			return;
		}
		
		// delete
		if (request.entity.getState().isLoaded()) {
			deleteForeignKeyInstance(op, request, request.entity.getState().getPersistedValue(fkProperty.getName()));
			return;
		}

		op.loader.load(request.entity, loaded -> deleteForeignKeyInstance(op, request, request.entity.getValue(fkProperty)));
	}
	
	private static void removeFromForeignTableCollection(DeleteRequest request, PropertyMetadata fkProperty, PropertyStaticMetadata foreignTableInfo) {
		if (request.entity.getState().isLoaded()) {
			Object foreignInstance = request.entity.getValue(fkProperty);
			if (foreignInstance != null && request.entity.isPersistedForeignKey(fkProperty, foreignInstance)) {
				try {
					ModelUtils.removeFromCollectionField(foreignTableInfo.getField(), foreignInstance, request.entity.getEntity());
				} catch (Exception e) {
					throw new ModelAccessException("Cannot remove instance from collection field", e);
				}
			}
		}
	}
	
	private void deleteForeignKeyInstance(Operation op, DeleteRequest request, Object foreignInstance) {
		if (foreignInstance == null)
			return;
		DeleteRequest deleteForeign = addToProcess(op, op.lcClient.getInstance(foreignInstance));
		deleteForeign.dependsOn(request);
	}
	
	@SuppressWarnings({"java:S3776"})
	@Override
	protected void processForeignTableField(Operation op, DeleteRequest request, PropertyStaticMetadata foreignTableInfo, @Nullable MutableObject<?> foreignFieldValue, PropertyMetadata fkProperty) {
		if (fkProperty.getForeignKeyAnnotation().onForeignDeleted().equals(ForeignKey.OnForeignDeleted.SET_TO_NULL)) {
			// update to null
			Object instId = request.entity.getRequiredPrimaryKey();
			request.dependsOn(op.updater.update(fkProperty, instId, null));
			if (foreignFieldValue != null) {
				Object foreignInstance = foreignFieldValue.getValue();
				if (foreignInstance != null)
					ModelUtils.setFieldValue(foreignInstance, fkProperty.getStaticMetadata().getField(), null);
				
			}
			return;
		}
		
		//delete
		if (foreignFieldValue != null && foreignTableInfo != null && request.entity.getState().getPersistedValue(foreignTableInfo.getField().getName()) == foreignFieldValue.getValue()) {
			// foreign loaded
			Object foreignInstance = foreignFieldValue.getValue();
			if (foreignInstance == null)
				return; // no link
			if (foreignTableInfo.isCollection()) {
				for (Object o : ModelUtils.getAsCollection(foreignFieldValue.getValue()))
					request.dependsOn(addToProcess(op, op.lcClient.getInstance(o)));
			} else {
				request.dependsOn(addToProcess(op, op.lcClient.getInstance(foreignFieldValue.getValue())));
			}
		} else {
			// foreign not loaded
			Object instId = request.entity.getRequiredPrimaryKey();
			if (!hasOtherLinks(fkProperty.getEntity(), fkProperty.getName())) {
				// can do delete where fk in (ids)
				request.dependsOn(op.deleteWithoutLoading.addRequest(fkProperty, instId));
			} else {
				// need to retrieve the entity from database, then process them to be deleted
				op.loader.retrieve(fkProperty, instId, loaded -> request.dependsOn(addToProcess(op, loaded)));
			}
		}
	}

	
	private static boolean hasOtherLinks(EntityMetadata entityType, String otherThanField) {
		for (PropertyStaticMetadata ft : entityType.getStaticMetadata().getForeignTables()) {
			if (!ft.getField().getName().equals(otherThanField))
				return true;
		}
		for (PropertyMetadata prop : entityType.getProperties()) {
			if (!prop.getName().equals(otherThanField) && prop.isForeignKey())
				return true;
		}
		return false;
	}
	
	@Override
	protected Mono<Void> doRequests(Operation op, EntityMetadata entityType, List<DeleteRequest> requests) {
		for (Iterator<DeleteRequest> it = requests.iterator(); it.hasNext(); ) {
			DeleteRequest r = it.next();
			if (!r.entity.getState().isPersisted()) {
				r.executed = true;
				it.remove();
			}
		}
		if (requests.isEmpty())
			return Mono.empty();
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
	
	private static Condition createCriteriaOnIds(EntityMetadata entityType, List<DeleteRequest> requests, SqlQuery<Delete> query, Table table) {
		List<Expression> ids = new ArrayList<>(requests.size());
		for (DeleteRequest request : requests) {
			PropertyMetadata idProperty = entityType.getRequiredIdProperty();
			Object id = request.entity.getValue(idProperty);
			ids.add(query.marker(query.getClient().getSchemaDialect().convertToDataBase(id, idProperty)));
		}
		return Conditions.in(Column.create(entityType.getRequiredIdProperty().getColumnName(), table), ids);
	}
	
	private static Condition createCriteriaOnProperties(EntityMetadata entityType, List<DeleteRequest> requests, SqlQuery<Delete> query, Table table) {
		Condition criteria = null;
		Iterator<DeleteRequest> it = requests.iterator();
		do {
			DeleteRequest request = it.next();
			Condition c = null;
			Iterator<PropertyMetadata> itProperty = entityType.getPersistentProperties().iterator();
			do {
				PropertyMetadata property = itProperty.next();
				Condition propertyCondition = createConditionOnProperty(table, property, request, query);
				c = c != null ? c.and(propertyCondition) : propertyCondition;
			} while (itProperty.hasNext());
			criteria = criteria != null ? criteria.or(c) : c;
		} while (it.hasNext());
		return criteria;
	}
	
	private static Condition createConditionOnProperty(Table table, PropertyMetadata property, DeleteRequest request, SqlQuery<Delete> query) {
		Object value = request.entity.getDatabaseValue(property);
		if (value == null)
			return Conditions.isNull(Column.create(property.getColumnName(), table));
		if (property.isForeignKey()) {
			// get the id instead of the entity
			value = request.getSavedForeignKeyValue(property.getName());
		}
		value = query.getClient().getSchemaDialect().convertToDataBase(value, property);
		return Conditions.isEqual(Column.create(property.getColumnName(), table), query.marker(value));
	}
	
	private static void deleteDone(EntityMetadata entityType, List<DeleteRequest> done) {
		for (DeleteRequest request : done) {
			// change the state of the entity instance
			request.entity.getState().deleted();
			// set id to null
			PropertyMetadata idProperty = entityType.getIdProperty();
			if (idProperty != null && !idProperty.getType().isPrimitive())
				request.entity.setValue(idProperty, null);
		}
	}

}
