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

import java.lang.reflect.Type;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import net.lecousin.reactive.data.relational.model.ModelAccessException;

/**
 * Information about an entity property, with specificities on a database connection (such as sql identifier).
 * 
 * @author Guillaume Le Cousin
 *
 */
public class PropertyMetadata {

	private EntityMetadata entity;
	private PropertyStaticMetadata meta;
	private RelationalPersistentProperty property;
	
	PropertyMetadata(EntityMetadata entity, PropertyStaticMetadata meta) {
		this.entity = entity;
		this.meta = meta;
		this.property = entity.getSpringMetadata().getPersistentProperty(meta.getName());
	}
	
	public @NonNull String getName() {
		return meta.getName();
	}
	
	public @NonNull Class<?> getType() {
		return meta.getType();
	}
	
	public @NonNull Type getGenericType() {
		return meta.getGenericType();
	}
	
	public @NonNull EntityMetadata getEntity() {
		return entity;
	}
	
	public @NonNull PropertyStaticMetadata getStaticMetadata() {
		return meta;
	}
	
	public @NonNull RelationalPersistentProperty getRequiredSpringProperty() {
		if (property != null)
			return property;
		throw new ModelAccessException("Property " + getName() + " is not a persistent property");
	}
	
	public @NonNull LcReactiveDataRelationalClient getClient() {
		return entity.getClient();
	}
	
	public boolean isPersistent() {
		return property != null && !property.isTransient();
	}
	
	public boolean isWritable() {
		return property != null && property.isWritable();
	}
	
	public boolean isForeignKey() {
		return meta.isForeignKey();
	}
	
	public ForeignKey getForeignKeyAnnotation() {
		return meta.getForeignKeyAnnotation();
	}
	
	public EntityMetadata getForeignKeyEntityMetadata() {
		return entity.getClient().getRequiredEntity(getType());
	}
	
	public boolean isForeignTable() {
		return meta.isForeignTable();
	}
	
	public ForeignTable getForeignTableAnnotation() {
		return meta.getForeignTableAnnotation();
	}
	
	public boolean isCollection() {
		return meta.isCollection();
	}
	
	public boolean isGeneratedValue() {
		return property != null && property.isAnnotationPresent(GeneratedValue.class);
	}
	
	public @NonNull GeneratedValue.Strategy getGeneratedValueStrategy() {
		return property.getRequiredAnnotation(GeneratedValue.class).strategy();
	}
	
	public @Nullable GeneratedValue getGeneratedValueAnnotation() {
		if (property == null)
			return null;
		return property.findAnnotation(GeneratedValue.class);
	}
	
	public @NonNull GeneratedValue getRequiredGeneratedValueAnnotation() {
		return property.getRequiredAnnotation(GeneratedValue.class);
	}
	
	public boolean isId() {
		return property != null && property.isIdProperty();
	}
	
	public boolean isVersion() {
		return property != null && property.isVersionProperty();
	}
	
	public boolean isCreatedDate() {
		return property != null && property.isAnnotationPresent(CreatedDate.class);
	}
	
	public boolean isLastModifiedDate() {
		return property != null && property.isAnnotationPresent(LastModifiedDate.class);
	}

	public SqlIdentifier getColumnName() {
		return property.getColumnName();
	}
	
	public boolean isUpdatable() {
		if (property == null)
			return false;
		if (!property.isWritable())
			return false;
		if (property.isIdProperty())
			return false;
		ColumnDefinition def = property.findAnnotation(ColumnDefinition.class);
		if (def != null)
			return def.updatable();
		return true;
	}

	/** Check if a property may be null.<br/>
	 * It cannot be null if:<ul>
	 * <li>the type is a primitive type</li>
	 * <li>this is the id property</li>
	 * <li>this is a foreign key, and it is specified as non optional</li>
	 * <li>this is not a foreign key, and the column definition specifies the column as nullable</li>
	 * </ul>
	 * 
	 * @param property
	 * @return
	 */
	public boolean isNullable() {
		if (property.getRawType().isPrimitive())
			return false;
		if (property.isIdProperty())
			return false;
		ForeignKey fk = property.findAnnotation(ForeignKey.class);
		if (fk != null)
			return fk.optional();
		ColumnDefinition def = property.findAnnotation(ColumnDefinition.class);
		if (def != null)
			return def.nullable();
		return true;
	}

}
