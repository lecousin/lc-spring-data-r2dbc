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
package net.lecousin.reactive.data.relational.model;

import net.lecousin.reactive.data.relational.annotations.CompositeId;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.metadata.EntityMetadata;
import net.lecousin.reactive.data.relational.model.metadata.EntityStaticMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyStaticMetadata;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.core.CollectionFactory;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * Utility methods.
 * 
 * @author Guillaume Le Cousin
 *
 */
public class ModelUtils {
	
	private ModelUtils() {
		// no instance
	}
	
	@SuppressWarnings("java:S3011")
	public static Object getFieldValue(@NonNull Object instance, @NonNull Field field) {
		try {
			return field.get(instance);
		} catch (IllegalAccessException e) {
			throw new ModelAccessException("Unable to get value from field " + field.getName() + " in class " + field.getDeclaringClass().getName(), e);
		}
	}
	
	@SuppressWarnings("java:S3011")
	public static void setFieldValue(@NonNull Object instance, @NonNull Field field, @Nullable Object value) {
		try {
			field.set(instance, value);
		} catch (IllegalAccessException e) {
			throw new ModelAccessException("Unable to set value into field " + field.getName() + " in class " + field.getDeclaringClass().getName() + " with value " + value, e);
		}
	}
	

	/** Set the foreign table field on the given instance to the given linkedInstance.
	 * 
	 * @param instance entity having the foreign table field
	 * @param linkedInstance entity having the foreign key
	 * @param linkedProperty foreign key property
	 */
	@SuppressWarnings("java:S3011")
	public static void setReverseLink(Object instance, Object linkedInstance, RelationalPersistentProperty linkedProperty) {
		PropertyStaticMetadata ft = EntityStaticMetadata.get(instance.getClass()).getForeignTableForJoinKey(linkedProperty.getName(), linkedInstance.getClass());
		if (ft != null && !ft.isCollection())
			setFieldValue(instance, ft.getField(), linkedInstance);
	}
	
	/** Retrieve all fields from the class and its super classes.
	 * 
	 * @param cl class
	 * @return fields
	 */
	public static List<Field> getAllFields(Class<?> cl) {
		List<Field> fields = new LinkedList<>();
		getAllFields(cl, fields);
		return fields;
	}
	
	private static void getAllFields(Class<?> cl, List<Field> fields) {
		if (cl == null)
			return;
		Collections.addAll(fields, cl.getDeclaredFields());
		getAllFields(cl.getSuperclass(), fields);
	}
	
	/** Check if the given field is a collection.
	 * 
	 * @param field field to check
	 * @return true if the field is an array or a Collection
	 */
	public static boolean isCollection(Field field) {
		return isCollectionType(field.getType());
	}
	
	/** Check if the given type is a collection.
	 * 
	 * @param type type to check
	 * @return true if the type is an array or implements Collection
	 */
	@SuppressWarnings("java:S1126")
	public static boolean isCollectionType(Class<?> type) {
		if (type.isArray())
			return !char[].class.equals(type);
		return Collection.class.isAssignableFrom(type);
	}
	
	/** Return the given object as a collection.
	 * 
	 * @param value the object
	 * @return a collection or null
	 */
	@SuppressWarnings({"unchecked", "java:S1168"})
	@Nullable
	public static <T> Collection<T> getAsCollection(Object value) {
		if (value instanceof Collection)
			return (Collection<T>) value;
		if (value.getClass().isArray()) {
			if (value.getClass().getComponentType().isPrimitive())
				return (Collection<T>)PrimitiveArraysUtil.primitiveArrayToCollection(value);
			return Arrays.asList((T[]) value);
		}
		return null;
	}
	
	/** Get the type of elements in a collection field.
	 * 
	 * @param field field
	 * @return type of elements
	 */
	@Nullable
	public static Class<?> getCollectionType(Field field) {
		if (field.getType().isArray())
			return field.getType().getComponentType();
		Type genType = field.getGenericType();
		if (genType instanceof ParameterizedType)
			return (Class<?>) ((ParameterizedType)genType).getActualTypeArguments()[0];
		return null;
	}
	
	/** Get the type of elements in a collection field.
	 * 
	 * @param field field
	 * @return type of elements
	 */
	public static Class<?> getRequiredCollectionType(Field field) {
		Class<?> collectionType = getCollectionType(field);
		if (collectionType != null)
			return collectionType;
		throw new MappingException("Field is not a collection: " + field.getDeclaringClass().getName() + "." + field.getName());
	}
	
	@SuppressWarnings({"unchecked", "java:S3011"})
	public static void addToCollectionField(Field field, Object collectionOwnerInstance, Object elementToAdd) throws IllegalAccessException {
		if (field.getType().isArray()) {
			Object[] array = (Object[]) field.get(collectionOwnerInstance);
			if (array == null) {
				array = (Object[]) Array.newInstance(field.getType().getComponentType(), 1);
				array[0] = elementToAdd;
				field.set(collectionOwnerInstance, array);
				return;
			}
			if (ArrayUtils.contains(array, elementToAdd))
				return;
			Object[] newArray = (Object[]) Array.newInstance(field.getType().getComponentType(), array.length + 1);
			System.arraycopy(array, 0, newArray, 0, array.length);
			newArray[array.length] = elementToAdd;
			field.set(collectionOwnerInstance, newArray);
			return;
		}
		Collection<Object> collectionInstance = (Collection<Object>) field.get(collectionOwnerInstance);
		if (collectionInstance == null) {
			collectionInstance = CollectionFactory.createCollection(field.getType(), getCollectionType(field), 10);
			field.set(collectionOwnerInstance, collectionInstance);
		}
		if (!collectionInstance.contains(elementToAdd))
			collectionInstance.add(elementToAdd);
	}
	
	@SuppressWarnings("java:S3011")
	public static void removeFromCollectionField(Field field, Object collectionOwnerInstance, Object elementToRemove) throws IllegalAccessException {
		if (field.getType().isArray()) {
			Object[] array = (Object[]) field.get(collectionOwnerInstance);
			if (array == null)
				return;
			int index = -1;
			for (int i = 0; i < array.length; ++i)
				if (array[i] == elementToRemove) {
					index = i;
					break;
				}
			if (index < 0)
				return;
			Object[] newArray = (Object[]) Array.newInstance(field.getType().getComponentType(), array.length - 1);
			if (index > 0)
				System.arraycopy(array, 0, newArray, 0, index);
			if (index < array.length - 1)
				System.arraycopy(array, index + 1, newArray, index, array.length - index - 1);
			field.set(collectionOwnerInstance, newArray);
			return;
		}
		@SuppressWarnings("unchecked")
		Collection<Object> collectionInstance = (Collection<Object>) field.get(collectionOwnerInstance);
		if (collectionInstance == null)
			return;
		collectionInstance.remove(elementToRemove);
	}
	
	
	public static Object getPersistedDatabaseValue(EntityState state, PropertyMetadata property) {
		Object value = state.getPersistedValue(property.getName());
		if (value == null)
			return null;
		if (property.isForeignKey()) {
			EntityMetadata e = property.getClient().getRequiredEntity(value.getClass());
			value = e.getSpringMetadata().getPropertyAccessor(value).getProperty(e.getRequiredIdProperty().getRequiredSpringProperty());
		}
		return value;
	}
	
	public static List<RelationalPersistentProperty> getProperties(RelationalPersistentEntity<?> entityType, String... names) {
		ArrayList<RelationalPersistentProperty> list = new ArrayList<>(names.length);
		for (String name : names)
			list.add(entityType.getRequiredPersistentProperty(name));
		return list;
	}
	
	public static boolean isPropertyPartOfCompositeId(RelationalPersistentProperty property) {
		CompositeId id = property.getOwner().findAnnotation(CompositeId.class);
		if (id == null)
			return false;
		return ArrayUtils.contains(id.properties(), property.getName());
	}
	
	
	
	public static Object getId(RelationalPersistentEntity<?> entityType, PropertiesSource source) {
		return idGetter(entityType).apply(source);
	}
	
	public static Function<PropertiesSource, Object> idGetter(RelationalPersistentEntity<?> entityType) {
		if (entityType.hasIdProperty())
			return idGetterFromIdProperty(entityType);
		if (entityType.isAnnotationPresent(CompositeId.class))
			return idGetterFromProperties(getProperties(entityType, entityType.getRequiredAnnotation(CompositeId.class).properties()));
		return idGetterFromProperties(entityType);
	}
	
	public static Function<PropertiesSource, Object> idGetterFromIdProperty(RelationalPersistentEntity<?> entityType) {
		RelationalPersistentProperty idProperty = entityType.getRequiredIdProperty();
		return source -> source.getPropertyValue(idProperty);
	}
	
	public static Function<PropertiesSource, Object> idGetterFromProperties(Iterable<RelationalPersistentProperty> properties) {
		return source -> {
			CompositeIdValue id = new CompositeIdValue();
			for (RelationalPersistentProperty property : properties) {
				id.add(property.getName(), source.getPropertyValue(property));
			}
			if (id.isNull())
				return null;
			return id;
		};
	}
	
	public static boolean hasCascadeDeleteImpacts(Class<?> entityType) {
		EntityStaticMetadata typeInfo = EntityStaticMetadata.get(entityType);
		if (typeInfo.hasForeignTable() || typeInfo.hasJoinTable())
			return true;
		for (PropertyStaticMetadata fk : typeInfo.getForeignKeys()) {
			if (fk.getForeignKeyAnnotation().cascadeDelete())
				return true;
			EntityStaticMetadata foreignInfo = EntityStaticMetadata.get(fk.getType());
			PropertyStaticMetadata ft = foreignInfo.getForeignTableForJoinKey(fk.getName(), entityType);
			if (ft != null && !ft.getForeignTableAnnotation().optional())
				return true;
		}
		return false;
	}
	
}
