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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.Transient;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.annotations.CompositeId;
import net.lecousin.reactive.data.relational.enhance.Enhancer;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelException;
import net.lecousin.reactive.data.relational.model.ModelUtils;

/**
 * Static information (common for all database connections) about an entity class.
 * 
 * @author Guillaume Le Cousin
 *
 */
public class EntityStaticMetadata {

	private static final Map<Class<?>, EntityStaticMetadata> cache = new HashMap<>();
	
	private Class<?> type;
	private Field stateField;
	private CompositeId compositeId;
	private Map<String, PropertyStaticMetadata> properties = new HashMap<>();
	private List<PropertyStaticMetadata> foreignKeys = new LinkedList<>();
	private List<PropertyStaticMetadata> foreignTables = new LinkedList<>();
	private List<PropertyStaticMetadata> joinTables = new LinkedList<>();
	
	public static void setClasses(Collection<Class<?>> classes) throws ModelException {
		for (Class<?> cl : classes)
			cache.put(cl, new EntityStaticMetadata(cl));
	}
	
	public static EntityStaticMetadata get(Class<?> clazz) {
		EntityStaticMetadata info = cache.get(clazz);
		if (info == null)
			throw new ModelAccessException("Unknown entity class " + clazz.getName() + ", known classes are: " + cache.keySet());
		return info;
	}
	
	public static Collection<Class<?>> getClasses() {
		return cache.keySet();
	}
	
	public static Collection<Class<?>> addGeneratedJoinTables(Collection<Class<?>> classes) {
		Set<Class<?>> result = new HashSet<>(classes);
		for (Class<?> c : classes) {
			EntityStaticMetadata info = get(c);
			for (PropertyStaticMetadata joinTable : info.joinTables) {
				Class<?> type = joinTable.getJoinTableForeignTable().getTypeOrCollectionElementType();
				result.add(type);
			}
		}
		return result;
	}
	
	@SuppressWarnings({"java:S3011", "java:S3776"})
	private EntityStaticMetadata(Class<?> clazz) throws ModelException {
		type = clazz;
		try {
			stateField = clazz.getDeclaredField(Enhancer.STATE_FIELD_NAME);
			stateField.setAccessible(true);
		} catch (Exception e) {
			throw new ModelException("Unable to access to state field for entity class " + clazz.getName());
		}
		compositeId = clazz.getAnnotation(CompositeId.class);
		List<Field> fields = ModelUtils.getAllFields(clazz);
		for (Field f : fields) {
			if (f.isAnnotationPresent(Transient.class) || f.isAnnotationPresent(Autowired.class) || f.isAnnotationPresent(Value.class))
				continue;
			PropertyStaticMetadata property = new PropertyStaticMetadata(f);
			properties.put(f.getName(), property);
			if (property.isForeignTable())
				foreignTables.add(property);
			else if (property.isForeignKey())
				foreignKeys.add(property);
		}
		for (PropertyStaticMetadata p : properties.values())
			if (p.isJoinTable()) {
				PropertyStaticMetadata joinForeignTable = properties.get(p.getName() + "_join");
				if (joinForeignTable == null)
					throw new ModelAccessException("@JoinTable without corresponding @ForeignTable"); // should never happen with Enhancer
				p.setJoinForeignTable(joinForeignTable);
				joinTables.add(p);
			}
		if (compositeId != null)
			for (String name : compositeId.properties())
				if (!properties.containsKey(name))
					throw new ModelAccessException("CompositeId property " + name + " does not exist on class " + clazz.getName());
	}
	
	public @NonNull Field getStateField() {
		return stateField;
	}
	
	public @NonNull Collection<PropertyStaticMetadata> getProperties() {
		return properties.values();
	}
	
	public @NonNull PropertyStaticMetadata getRequiredProperty(String name) {
		PropertyStaticMetadata p = properties.get(name);
		if (p != null)
			return p;
		throw new ModelAccessException("Unknown property " + name + " in " + type.getName());
	}
	
	public boolean hasCompositeId() {
		return compositeId != null;
	}
	
	public @Nullable CompositeId getCompositeId() {
		return compositeId;
	}
	
	public @NonNull CompositeId getRequiredCompositeId() {
		if (compositeId == null)
			throw new ModelAccessException("Entity " + type.getName() + " doesn't have a @CompositeId");
		return compositeId;
	}
	
	public boolean hasForeignTable() {
		return !foreignTables.isEmpty();
	}
	
	public boolean hasJoinTable() {
		return !joinTables.isEmpty();
	}
	
	public List<PropertyStaticMetadata> getForeignKeys() {
		return Collections.unmodifiableList(foreignKeys);
	}
	
	public List<PropertyStaticMetadata> getForeignTables() {
		return Collections.unmodifiableList(foreignTables);
	}
	
	/** Return the foreign table property having the given join key on the given target type.
	 * 
	 * @param joinKey join key
	 * @param targetType type of target entity
	 * @return the property
	 */
	public @Nullable PropertyStaticMetadata getForeignTableForJoinKey(String joinKey, Class<?> targetType) {
		for (PropertyStaticMetadata p : foreignTables) {
			if (p.getForeignTableAnnotation().joinKey().equals(joinKey) && p.getTypeOrCollectionElementType().equals(targetType))
				return p;
		}
		return null;
	}

	@SuppressWarnings({"unchecked", "java:S1168", "java:S2259"})
	public <T> Collection<T> getJoinTableElementsForJoinTableClass(Object instance, Class<T> joinTableClass) {
		for (PropertyStaticMetadata jt : joinTables) {
			Field field = jt.getJoinTableForeignTable().getField();
			if (ModelUtils.getCollectionType(field).equals(joinTableClass)) {
				try {
					return (Collection<T>) field.get(instance);
				} catch (Exception e) {
					throw new ModelAccessException("Error accessing join table elements " + joinTableClass.getName() + " from " + instance, e);
				}
			}
		}
		return null;
	}

}
