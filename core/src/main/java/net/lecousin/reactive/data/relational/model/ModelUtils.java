package net.lecousin.reactive.data.relational.model;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.core.CollectionFactory;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.CompositeId;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.query.SqlQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;

public class ModelUtils {
	
	private static final Map<Class<?>, Map<String, Pair<Field, ForeignTable>>> CACHE_FOREIGN_TABLE = new HashMap<>();

	private ModelUtils() {
		// no instance
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
	public static boolean isNullable(RelationalPersistentProperty property) {
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

	/** Check if a property may be updated.
	 * 
	 * @param property
	 * @return
	 */
	public static boolean isUpdatable(RelationalPersistentProperty property) {
		if (!property.isWritable())
			return false;
		if (property.isIdProperty())
			return false;
		ColumnDefinition def = property.findAnnotation(ColumnDefinition.class);
		if (def != null)
			return def.updatable();
		return true;
	}
	
	/** Set the foreign table field on the given instance to the given linkedInstance.
	 * 
	 * @param instance entity having the foreign table field
	 * @param linkedInstance entity having the foreign key
	 * @param linkedProperty foreign key property
	 */
	@SuppressWarnings("java:S3011")
	public static void setReverseLink(Object instance, Object linkedInstance, RelationalPersistentProperty linkedProperty) {
		Field field = getForeignTableFieldForJoinKey(instance.getClass(), linkedProperty.getName(), linkedInstance.getClass());
		if (field != null && !isCollection(field))
			try {
				field.set(instance, linkedInstance);
			} catch (Exception e) {
				throw new ModelAccessException("Unable to set ForeignTable field " + field.getName() + " on " + instance.getClass().getSimpleName() + " with value " + linkedInstance, e);
			}
	}
	
	/** Return the foreign table field on the given entity type, having the given join key.
	 * 
	 * @param entity entity type on which to search for the foreign table field
	 * @param joinKey join key
	 * @return the field
	 */
	@Nullable
	public static Field getForeignTableFieldForJoinKey(Class<?> entity, String joinKey, Class<?> targetType) {
		Pair<Field, ForeignTable> p = getForeignTableWithFieldForJoinKey(entity, joinKey, targetType);
		return p != null ? p.getFirst() : null;
	}
	
	/** Return the foreign table field on the given entity type, having the given join key.
	 * 
	 * @param entity entity type on which to search for the foreign table field
	 * @param joinKey join key
	 * @return the field
	 */
	public static Field getRequiredForeignTableFieldForJoinKey(Class<?> entity, String joinKey, Class<?> targetType) {
		return getRequiredForeignTableWithFieldForJoinKey(entity, joinKey, targetType).getFirst();
	}
	
	/** Return the foreign table field on the given entity type, having the given join key.
	 * 
	 * @param entity entity type on which to search for the foreign table field
	 * @param joinKey join key
	 * @return the field and the foreign table annotation
	 */
	@Nullable
	public static Pair<Field, ForeignTable> getForeignTableWithFieldForJoinKey(Class<?> entity, String joinKey, Class<?> targetType) {
		Map<String, Pair<Field, ForeignTable>> map = getForeignTableFieldMap(entity);
		for (Map.Entry<String, Pair<Field, ForeignTable>> e : map.entrySet())
			if (e.getValue().getSecond().joinKey().equals(joinKey)) {
				Field field = e.getValue().getFirst();
				Class<?> type;
				if (ModelUtils.isCollection(field))
					type = ModelUtils.getCollectionType(field);
				else
					type = field.getType();
				if (targetType.equals(type))
					return e.getValue();
			}
		return null;
	}
	
	private static String missingForeignTable(String expected, String expectedOn, Class<?> entity) {
		return "Missing @ForeignTable " + expected + " '" + expectedOn + "' in class '" + entity.getSimpleName() + "'";
	}
	
	/** Return the foreign table field on the given entity type, having the given join key.
	 * 
	 * @param entity entity type on which to search for the foreign table field
	 * @param joinKey join key
	 * @return the field and the foreign table annotation
	 */
	public static Pair<Field, ForeignTable> getRequiredForeignTableWithFieldForJoinKey(Class<?> entity, String joinKey, Class<?> targetType) {
		Pair<Field, ForeignTable> p = getForeignTableWithFieldForJoinKey(entity, joinKey, targetType);
		if (p == null)
			throw new MappingException(missingForeignTable("field with join key", joinKey, entity));
		return p;
	}
	
	/** Return the foreign table field on the given property in the given entity type.
	 * 
	 * @param entity entity type on which to search for the foreign table field
	 * @param propertyName foreign table property
	 * @return the field
	 */
	@Nullable
	public static Field getForeignTableFieldForProperty(Class<?> entity, String propertyName) {
		Map<String, Pair<Field, ForeignTable>> map = getForeignTableFieldMap(entity);
		Pair<Field, ForeignTable> p = map.get(propertyName);
		return p != null ? p.getFirst() : null;
	}
	
	/** Return the foreign table field on the given property in the given entity type.
	 * 
	 * @param entity entity type on which to search for the foreign table field
	 * @param propertyName foreign table property
	 * @return the field
	 */
	public static Field getRequiredForeignTableFieldForProperty(Class<?> entity, String propertyName) {
		Map<String, Pair<Field, ForeignTable>> map = getForeignTableFieldMap(entity);
		Pair<Field, ForeignTable> p = map.get(propertyName);
		if (p == null)
			throw new MappingException(missingForeignTable("on property", propertyName, entity));
		return p.getFirst();
	}
	
	/** Return the foreign table field on the given property in the given entity type.
	 * 
	 * @param entity entity type on which to search for the foreign table field
	 * @param propertyName foreign table property
	 * @return the foreign table annotation
	 */
	@Nullable
	public static ForeignTable getForeignTableForProperty(Class<?> entity, String propertyName) {
		Map<String, Pair<Field, ForeignTable>> map = getForeignTableFieldMap(entity);
		Pair<Field, ForeignTable> p = map.get(propertyName);
		return p != null ? p.getSecond() : null;
	}
	
	/** Return the foreign table field on the given property in the given entity type.
	 * 
	 * @param entity entity type on which to search for the foreign table field
	 * @param propertyName foreign table property
	 * @return the foreign table annotation
	 */
	public static ForeignTable getRequiredForeignTableForProperty(Class<?> entity, String propertyName) {
		Map<String, Pair<Field, ForeignTable>> map = getForeignTableFieldMap(entity);
		Pair<Field, ForeignTable> p = map.get(propertyName);
		if (p == null)
			throw new MappingException(missingForeignTable("on property", propertyName, entity));
		return p.getSecond();
	}
	
	/** Return true if the given field is associated to a @ForeignTable annotation.
	 * 
	 * @param field field
	 * @return true if it is a foreign table
	 */
	public static boolean isForeignTableField(Field field) {
		Map<String, Pair<Field, ForeignTable>> map = getForeignTableFieldMap(field.getDeclaringClass());
		for (Pair<Field, ForeignTable> p : map.values())
			if (p.getFirst().equals(field))
				return true;
		return false;
	}
	
	/** Return the list of foreign tables declared in the given entity type.
	 * 
	 * @param entity entity type
	 * @return list of fields with their corresponding foreign table annotation
	 */
	public static Collection<Pair<Field, ForeignTable>> getForeignTables(Class<?> entity) {
		return getForeignTableFieldMap(entity).values();
	}
	
	private static Map<String, Pair<Field, ForeignTable>> getForeignTableFieldMap(Class<?> entity) {
		Map<String, Pair<Field, ForeignTable>> map;
		synchronized (CACHE_FOREIGN_TABLE) {
			map = CACHE_FOREIGN_TABLE.get(entity);
			if (map == null) {
				map = new HashMap<>();
				fillForeignTables(entity, map);
				CACHE_FOREIGN_TABLE.put(entity, map);
			}
		}
		return map;
	}
	
	@SuppressWarnings("java:S3011")
	private static void fillForeignTables(Class<?> entity, Map<String, Pair<Field, ForeignTable>> map) {
		for (Field f : getAllFields(entity)) {
			ForeignTable ft = f.getAnnotation(ForeignTable.class);
			if (ft != null) {
				map.put(f.getName(), Pair.of(f, ft));
				f.setAccessible(true);
			}
		}
	}
	
	private static List<Field> getAllFields(Class<?> cl) {
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
	
	/** Return the identifier for the given entity.
	 * 
	 * @param instance entity
	 * @param entityType entity type
	 * @return identifier
	 */
	public static Object getRequiredId(Object instance, RelationalPersistentEntity<?> entityType, @Nullable PersistentPropertyAccessor<?> accessor) {
		RelationalPersistentProperty idProperty = entityType.getRequiredIdProperty();
		Object id = (accessor != null ? accessor : entityType.getPropertyAccessor(instance)).getProperty(idProperty);
		if (id == null)
			throw new InvalidEntityStateException("Entity is supposed to be persisted to database, but it's Id property is null");
		return id;
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
		if (Collection.class.isAssignableFrom(type))
			return true;
		return false;
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
		if (value.getClass().isArray())
			return Arrays.asList((T[]) value);
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
		if (field.getType().isArray())
			return field.getType().getComponentType();
		Type genType = field.getGenericType();
		if (genType instanceof ParameterizedType)
			return (Class<?>) ((ParameterizedType)genType).getActualTypeArguments()[0];
		throw new MappingException("Field is not a collection: " + field.getDeclaringClass().getName() + "." + field.getName());
	}
	
	@SuppressWarnings({"unchecked", "java:S3011"})
	public static void addToCollectionField(Field field, Object collectionOwnerInstance, Object elementToAdd) throws IllegalAccessException {
		if (field.getType().isArray()) {
			Object[] array = (Object[]) field.get(collectionOwnerInstance);
			if (array == null || array.length == 0) {
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
	
	@SuppressWarnings("java:S3011")
	public static Object getDatabaseValue(Object instance, RelationalPersistentProperty property, MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext) {
		Field f = property.getRequiredField();
		f.setAccessible(true);
		Object value;
		try {
			value = f.get(instance);
		} catch (IllegalAccessException e) {
			throw new ModelAccessException("Unable to get field value", e);
		}
		if (value == null)
			return null;
		if (property.isAnnotationPresent(ForeignKey.class)) {
			RelationalPersistentEntity<?> e = mappingContext.getRequiredPersistentEntity(value.getClass());
			value = e.getPropertyAccessor(value).getProperty(e.getRequiredIdProperty());
		}
		return value;
	}
	
	public static Object getPersistedDatabaseValue(EntityState state, RelationalPersistentProperty property, MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext) {
		Object value = state.getPersistedValue(property.getName());
		if (value == null)
			return null;
		if (property.isAnnotationPresent(ForeignKey.class)) {
			RelationalPersistentEntity<?> e = mappingContext.getRequiredPersistentEntity(value.getClass());
			value = e.getPropertyAccessor(value).getProperty(e.getRequiredIdProperty());
		}
		return value;
	}
	
	public static List<RelationalPersistentProperty> getProperties(RelationalPersistentEntity<?> entityType, String... names) {
		ArrayList<RelationalPersistentProperty> list = new ArrayList<>(names.length);
		for (String name : names)
			list.add(entityType.getRequiredPersistentProperty(name));
		return list;
	}
	
	public static Object getId(RelationalPersistentEntity<?> entityType, PersistentPropertyAccessor<?> accessor, MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext) {
		if (entityType.hasIdProperty())
			return getIdPropertyValue(entityType, accessor);
		if (entityType.isAnnotationPresent(CompositeId.class))
			return getIdFromProperties(getProperties(entityType, entityType.getRequiredAnnotation(CompositeId.class).properties()), accessor, mappingContext);
		return getIdFromProperties(entityType, accessor, mappingContext);
	}
	
	public static Object getIdPropertyValue(RelationalPersistentEntity<?> entityType, PersistentPropertyAccessor<?> accessor) {
		return accessor.getProperty(entityType.getRequiredIdProperty());
	}
	
	public static CompositeIdValue getIdFromProperties(Iterable<RelationalPersistentProperty> properties, PersistentPropertyAccessor<?> accessor, MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext) {
		CompositeIdValue id = new CompositeIdValue();
		for (RelationalPersistentProperty property : properties) {
			id.add(property.getName(), getDatabaseValue(accessor.getBean(), property, mappingContext));
		}
		return id;
	}
	
	public static Object getId(RelationalPersistentEntity<?> entityType, PropertiesSource source) {
		if (entityType.hasIdProperty())
			return getIdPropertyValue(entityType, source);
		if (entityType.isAnnotationPresent(CompositeId.class))
			return getIdFromProperties(getProperties(entityType, entityType.getRequiredAnnotation(CompositeId.class).properties()), source);
		return getIdFromProperties(entityType, source);
	}
	
	public static Object getIdPropertyValue(RelationalPersistentEntity<?> entityType, PropertiesSource source) {
		return source.getPropertyValue(entityType.getRequiredIdProperty());
	}
	
	public static CompositeIdValue getIdFromProperties(Iterable<RelationalPersistentProperty> properties, PropertiesSource source) {
		CompositeIdValue id = new CompositeIdValue();
		for (RelationalPersistentProperty property : properties) {
			id.add(property.getName(), source.getPropertyValue(property));
		}
		return id;
	}
	
	public static Condition getConditionOnId(SqlQuery<?> query, RelationalPersistentEntity<?> entityType, PersistentPropertyAccessor<?> accessor, MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext) {
		if (entityType.hasIdProperty())
			return getConditionOnProperties(query, entityType, Arrays.asList(entityType.getRequiredIdProperty()), accessor, mappingContext);
		if (entityType.isAnnotationPresent(CompositeId.class))
			return getConditionOnProperties(query, entityType, getProperties(entityType, entityType.getRequiredAnnotation(CompositeId.class).properties()), accessor, mappingContext);
		return getConditionOnProperties(query, entityType, entityType, accessor, mappingContext);
	}
	
	public static Condition getConditionOnProperties(SqlQuery<?> query, RelationalPersistentEntity<?> entityType, Iterable<RelationalPersistentProperty> properties, PersistentPropertyAccessor<?> accessor, MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext) {
		Iterator<RelationalPersistentProperty> it = properties.iterator();
		Condition condition = null;
		Table table = Table.create(entityType.getTableName());
		do {
			RelationalPersistentProperty property = it.next();
			Object value = getDatabaseValue(accessor.getBean(), property, mappingContext);
			Condition propertyCondition = Conditions.isEqual(Column.create(property.getColumnName(), table), value != null ? query.marker(value) : SQL.nullLiteral());
			condition = condition != null ? condition.and(propertyCondition) : propertyCondition;
		} while (it.hasNext());
		return condition;
	}
	
	public static Criteria getCriteriaOnId(String entityName, RelationalPersistentEntity<?> entityType, PersistentPropertyAccessor<?> accessor, MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext) {
		if (entityType.hasIdProperty())
			return getCriteriaOnProperties(entityName, Arrays.asList(entityType.getRequiredIdProperty()), accessor, mappingContext);
		if (entityType.isAnnotationPresent(CompositeId.class))
			return getCriteriaOnProperties(entityName, getProperties(entityType, entityType.getRequiredAnnotation(CompositeId.class).properties()), accessor, mappingContext);
		return getCriteriaOnProperties(entityName, entityType, accessor, mappingContext);
	}
	
	public static Criteria getCriteriaOnProperties(String entityName, Iterable<RelationalPersistentProperty> properties, PersistentPropertyAccessor<?> accessor, MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext) {
		Iterator<RelationalPersistentProperty> it = properties.iterator();
		Criteria condition = null;
		do {
			RelationalPersistentProperty property = it.next();
			Object value = getDatabaseValue(accessor.getBean(), property, mappingContext);
			Criteria propertyCondition = value != null ? Criteria.property(entityName, property.getName()).is(value) : Criteria.property(entityName, property.getName()).isNull();
			condition = condition != null ? condition.and(propertyCondition) : propertyCondition;
		} while (it.hasNext());
		return condition;
	}
	
}
