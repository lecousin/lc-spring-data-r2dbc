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
package net.lecousin.reactive.data.relational.model.metadata;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.CompositeIdValue;
import net.lecousin.reactive.data.relational.model.InvalidEntityStateException;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.query.SqlQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.util.Iterables;

/**
 * Entity instance with state and metadata.
 * 
 * @author Guillaume Le Cousin
 *
 * @param <T> type of entity
 */
public class EntityInstance<T> {

	private T instance;
	private EntityState state;
	private PersistentPropertyAccessor<T> accessor;
	private EntityMetadata meta;

	
	public EntityInstance(@NonNull T instance, @NonNull EntityState state) {
		this.instance = instance;
		this.state = state;
		this.meta = state.getMetadata();
	}
	
	public @NonNull T getEntity() {
		return instance;
	}
	
	public @NonNull EntityState getState() {
		return state;
	}
	
	public @NonNull PersistentPropertyAccessor<T> getPropertyAccessor() {
		if (accessor == null)
			accessor = meta.getSpringMetadata().getPropertyAccessor(instance);
		return accessor;
	}
	
	public @NonNull Class<?> getType() {
		return meta.getType();
	}
	
	public @NonNull EntityMetadata getMetadata() {
		return meta;
	}

	public Object getValue(String propertyName) {
		return getValue(meta.getRequiredProperty(propertyName));
	}
	
	public Object getValue(PropertyMetadata property) {
		if (property.isPersistent())
			return getPropertyAccessor().getProperty(property.getRequiredSpringProperty());
		return ModelUtils.getFieldValue(instance, property.getStaticMetadata().getField());
	}
	
	/** Returns the value stored in database for the given property. */
	public Object getDatabaseValue(String propertyName) {
		return getDatabaseValue(meta.getRequiredProperty(propertyName));
	}
	
	/** Returns the value stored in database for the given property. */
	public Object getDatabaseValue(PropertyMetadata property) {
		Object value = ModelUtils.getFieldValue(instance, property.getStaticMetadata().getField());
		if (value == null)
			return null;
		if (property.isForeignKey())
			return getForeignKeyValue(property, value);
		value = state.getClient().getSchemaDialect().convertToDataBase(value, property);
		return value;
	}
	
	public void setValue(PropertyMetadata property, Object value) {
		if (property.isPersistent())
			getPropertyAccessor().setProperty(property.getRequiredSpringProperty(), value);
		else
			ModelUtils.setFieldValue(instance, property.getStaticMetadata().getField(), value);
	}
	
	/**
	 * Returns an object representing the unique id of the entity:<ul>
	 * <li>If the entity has an @Id property it returns its value</li>
	 * <li>If the entity has a @CompositeId it returns a {@link CompositeIdValue}</li>
	 * <li>Else it returns a {@link CompositeIdValue} with all persisted properties</li>
	 * </ul>
	 */
	public @Nullable Object getId() {
		if (meta.hasIdProperty())
			return getIdPropertyValue();
		if (meta.hasCompositeId())
			return getIdFromProperties(meta.getCompositeIdProperties());
		return getIdFromProperties(meta.getPersistentProperties());
	}
	
	/** Return the value of the @Id property, or null. */
	public @Nullable Object getIdPropertyValue() {
		return getPropertyAccessor().getProperty(meta.getRequiredIdProperty().getRequiredSpringProperty());
	}

	private CompositeIdValue getIdFromProperties(Iterable<PropertyMetadata> properties) {
		CompositeIdValue id = new CompositeIdValue();
		for (PropertyMetadata property : properties) {
			id.add(property.getName(), getDatabaseValue(property));
		}
		return id;
	}
	
	/** Return the primary key. */
	public @NonNull Object getRequiredPrimaryKey() {
		Object id = getValue(meta.getRequiredIdProperty());
		if (id == null)
			throw new InvalidEntityStateException("Entity is supposed to be persisted to database, but it's Id property is null");
		return id;
	}
	
	/** Return true if the persisted foreign id is equals to the id of the foreignInstance (or both null). */
	public boolean isPersistedForeignKey(PropertyMetadata fkProperty, Object foreignInstance) {
		Object persisted = state.getPersistedValue(fkProperty.getName());
		if (foreignInstance == null)
			return persisted == null;
		if (persisted == null)
			return false;
		return Objects.equals(getForeignKeyValue(fkProperty, persisted), getForeignKeyValue(fkProperty, foreignInstance));
	}
	
	/** Return the primary key from a foreign key instance. */
	public Object getForeignKeyValue(PropertyMetadata fkProperty, Object foreignInstance) {
		if (foreignInstance == null)
			return null;
		EntityMetadata fkType = fkProperty.getForeignKeyEntityMetadata();
		return ModelUtils.getFieldValue(foreignInstance, fkType.getRequiredIdProperty().getStaticMetadata().getField());
	}


	public Criteria getCriteriaOnId(String entityName) {
		if (meta.hasIdProperty())
			return getCriteriaOnProperties(entityName, Arrays.asList(meta.getRequiredIdProperty().getName()));
		if (meta.hasCompositeId())
			return getCriteriaOnProperties(entityName, Arrays.asList(meta.getRequiredCompositeId().properties()));
		return getCriteriaOnProperties(entityName, Iterables.map(meta.getPersistentProperties(), PropertyMetadata::getName));
	}
	
	private Criteria getCriteriaOnProperties(String entityName, Iterable<String> properties) {
		Iterator<String> it = properties.iterator();
		Criteria condition = null;
		do {
			String propertyName = it.next();
			Object value = getDatabaseValue(propertyName);
			Criteria propertyCondition = value != null ? Criteria.property(entityName, propertyName).is(value) : Criteria.property(entityName, propertyName).isNull();
			condition = condition != null ? condition.and(propertyCondition) : propertyCondition;
		} while (it.hasNext());
		return condition;
	}

	public Condition getConditionOnId(SqlQuery<?> query) {
		if (meta.hasIdProperty())
			return getConditionOnProperties(query, Arrays.asList(meta.getRequiredIdProperty()));
		if (meta.hasCompositeId())
			return getConditionOnProperties(query, meta.getCompositeIdProperties());
		return getConditionOnProperties(query, meta.getPersistentProperties());
	}
	
	private Condition getConditionOnProperties(SqlQuery<?> query, Iterable<PropertyMetadata> properties) {
		Iterator<PropertyMetadata> it = properties.iterator();
		Condition condition = null;
		Table table = Table.create(meta.getTableName());
		do {
			PropertyMetadata property = it.next();
			Object value = getDatabaseValue(property);
			Condition propertyCondition = Conditions.isEqual(Column.create(property.getColumnName(), table), value != null ? query.marker(value) : SQL.nullLiteral());
			condition = condition != null ? condition.and(propertyCondition) : propertyCondition;
		} while (it.hasNext());
		return condition;
	}

}
