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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.annotations.CompositeId;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.util.Iterables;

/**
 * Information about an entity class, with specificities to a database connection.
 * 
 * @author Guillaume Le Cousin
 *
 */
public class EntityMetadata {

	private @NonNull LcReactiveDataRelationalClient client;
	private @NonNull EntityStaticMetadata meta;
	private @NonNull RelationalPersistentEntity<?> entity;
	private Map<String, PropertyMetadata> properties = new HashMap<>();
	private PropertyMetadata idProperty;
	private PropertyMetadata versionProperty;
	
	public EntityMetadata(@NonNull LcReactiveDataRelationalClient client, @NonNull RelationalPersistentEntity<?> entity) {
		this.client = client;
		this.entity = entity;
		this.meta = EntityStaticMetadata.get(entity.getType());
		for (PropertyStaticMetadata property : meta.getProperties())
			properties.put(property.getName(), new PropertyMetadata(this, property));
		if (entity.hasIdProperty())
			idProperty = properties.get(entity.getRequiredIdProperty().getName());
		if (entity.hasVersionProperty())
			versionProperty = properties.get(entity.getRequiredVersionProperty().getName());
	}
	
	public @NonNull String getName() {
		return entity.getName();
	}
	
	public @NonNull LcReactiveDataRelationalClient getClient() {
		return client;
	}
	
	public @NonNull EntityStaticMetadata getStaticMetadata() {
		return meta;
	}
	
	@SuppressWarnings("java:S1452")
	public @NonNull RelationalPersistentEntity<?> getSpringMetadata() {
		return entity;
	}
	
	public @NonNull Class<?> getType() {
		return entity.getType();
	}
	
	public @Nullable PropertyMetadata getProperty(String name) {
		return properties.get(name);
	}
	
	public @NonNull PropertyMetadata getRequiredProperty(String name) {
		PropertyMetadata p = properties.get(name);
		if (p != null)
			return p;
		throw new ModelAccessException("Unknown property " + name + " in " + getName());
	}
	
	public @NonNull Collection<PropertyMetadata> getProperties() {
		return properties.values();
	}
	
	public @NonNull Iterable<PropertyMetadata> getPersistentProperties() {
		return Iterables.filter(properties.values(), PropertyMetadata::isPersistent);
	}
	
	public @NonNull PropertyMetadata getRequiredPersistentProperty(String name) {
		PropertyMetadata p = getRequiredProperty(name);
		if (!p.isPersistent())
			throw new ModelAccessException("Property " + name + " is not persistent (no corresponding column in the table) on class " + getType().getName());
		return p;
	}
	
	public Iterable<PropertyMetadata> getForeignKeys() {
		return Iterables.filter(properties.values(), PropertyMetadata::isForeignKey);
	}
	
	public @NonNull PropertyMetadata getRequiredForeignTableProperty(String name) {
		PropertyMetadata p = getRequiredProperty(name);
		if (!p.isForeignTable())
			throw new ModelAccessException("Property " + name + " is not a @ForeignTable on class " + getType().getName());
		return p;
	}
	
	public Iterable<PropertyMetadata> getGeneratedValues() {
		return Iterables.filter(properties.values(), PropertyMetadata::isGeneratedValue);
	}

	public boolean hasIdProperty() {
		return idProperty != null;
	}
	
	public @Nullable PropertyMetadata getIdProperty() {
		return idProperty;
	}
	
	public @NonNull PropertyMetadata getRequiredIdProperty() {
		if (idProperty != null)
			return idProperty;
		throw new ModelAccessException("Entity " + getName() + " does not have an @Id property");
	}
	
	public boolean hasCompositeId() {
		return meta.hasCompositeId();
	}
	
	public @Nullable CompositeId getCompositeIdAnnotation() {
		return meta.getCompositeId();
	}
	
	public @NonNull CompositeId getRequiredCompositeId() {
		return meta.getRequiredCompositeId();
	}
	
	public List<PropertyMetadata> getCompositeIdProperties() {
		String[] names = meta.getRequiredCompositeId().properties();
		ArrayList<PropertyMetadata> list = new ArrayList<>(names.length);
		for (String name : names)
			list.add(properties.get(name));
		return list;
	}

	public @NonNull SqlIdentifier getTableName() {
		return entity.getTableName();
	}
	
	public boolean hasVersionProperty() {
		return versionProperty != null;
	}
	
	public @Nullable PropertyMetadata getVersionProperty() {
		return versionProperty;
	}
	
	public @NonNull PropertyMetadata getRequiredVersionProperty() {
		if (versionProperty != null)
			return versionProperty;
		throw new ModelAccessException("Entity " + getName() + " does not have a @Version property");
	}
}
