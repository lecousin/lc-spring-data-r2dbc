package net.lecousin.reactive.data.relational.model;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mapping.MappingException;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.JoinTable;
import net.lecousin.reactive.data.relational.enhance.Enhancer;

public class LcEntityTypeInfo {

	private static final Map<Class<?>, LcEntityTypeInfo> cache = new HashMap<>();
	
	private Class<?> type;
	private Field stateField;
	private Map<String, ForeignTableInfo> foreignTables = new HashMap<>();
	private Map<String, JoinTableInfo> joinTables = new HashMap<>();
	
	public static class ForeignTableInfo {
		private Field field;
		private ForeignTable annotation;
		
		private ForeignTableInfo(Field field, ForeignTable annotation) {
			this.field = field;
			this.annotation = annotation;
		}

		public Field getField() {
			return field;
		}

		public ForeignTable getAnnotation() {
			return annotation;
		}
	}
	
	public static class JoinTableInfo {
		private Field field;
		private JoinTable annotation;
		private ForeignTableInfo joinForeignTable;
		private String joinSourceFieldName;
		private String joinTargetFieldName;

		public Field getField() {
			return field;
		}
		
		public JoinTable getAnnotation() {
			return annotation;
		}
		
		public ForeignTableInfo getJoinForeignTable() {
			return joinForeignTable;
		}

		public String getJoinSourceFieldName() {
			return joinSourceFieldName;
		}

		public String getJoinTargetFieldName() {
			return joinTargetFieldName;
		}
		
	}
	
	public static void setClasses(Collection<Class<?>> classes) throws ModelException {
		for (Class<?> cl : classes)
			cache.put(cl, new LcEntityTypeInfo(cl));
	}
	
	public static LcEntityTypeInfo get(Class<?> clazz) {
		LcEntityTypeInfo info = cache.get(clazz);
		if (info == null)
			throw new ModelAccessException("Unknown entity class " + clazz.getName());
		return info;
	}
	
	public static Collection<Class<?>> getClasses() {
		return cache.keySet();
	}
	
	@SuppressWarnings({"squid:S3011"})
	private LcEntityTypeInfo(Class<?> clazz) throws ModelException {
		type = clazz;
		try {
			stateField = clazz.getDeclaredField(Enhancer.STATE_FIELD_NAME);
			stateField.setAccessible(true);
		} catch (Exception e) {
			throw new ModelException("Unable to access to state field for entity class " + clazz.getName());
		}
		List<Field> fields = ModelUtils.getAllFields(clazz);
		for (Field f : fields) {
			ForeignTable ft = f.getAnnotation(ForeignTable.class);
			if (ft != null) {
				foreignTables.put(f.getName(), new ForeignTableInfo(f, ft));
				f.setAccessible(true);
			}
		}
		for (Field f : fields) {
			JoinTable jt = f.getAnnotation(JoinTable.class);
			if (jt != null) {
				JoinTableInfo info = new JoinTableInfo();
				info.field = f;
				info.annotation = jt;
				info.joinForeignTable = foreignTables.get(f.getName() + "_join");
				if (info.joinForeignTable == null)
					throw new ModelAccessException("@JoinTable without corresponding @ForeignTable"); // should never happen with Enhancer
				if (info.joinForeignTable.annotation.joinKey().equals("entity1")) {
					info.joinSourceFieldName = "entity1";
					info.joinTargetFieldName = "entity2";
				} else {
					info.joinSourceFieldName = "entity2";
					info.joinTargetFieldName = "entity1";
				}
				joinTables.put(f.getName(), info);
				f.setAccessible(true);
			}
		}		
	}
	
	public Field getStateField() {
		return stateField;
	}

	/** Return the foreign table field having the given join key.
	 * 
	 * @param joinKey join key
	 * @param targetType type of target entity
	 * @return the field
	 */
	@Nullable
	public Field getForeignTableFieldForJoinKey(String joinKey, Class<?> targetType) {
		ForeignTableInfo i = getForeignTableWithFieldForJoinKey(joinKey, targetType);
		return i != null ? i.getField() : null;
	}
	
	/** Return the foreign table field having the given join key.
	 * 
	 * @param joinKey join key
	 * @param targetType type of target entity
	 * @return the field
	 */
	public Field getRequiredForeignTableFieldForJoinKey(String joinKey, Class<?> targetType) {
		return getRequiredForeignTableWithFieldForJoinKey(joinKey, targetType).getField();
	}
	
	/** Return the foreign table field having the given join key.
	 * 
	 * @param joinKey join key
	 * @param targetType type of target entity
	 * @return the field and the foreign table annotation
	 */
	@Nullable
	public ForeignTableInfo getForeignTableWithFieldForJoinKey(String joinKey, Class<?> targetType) {
		for (Map.Entry<String, ForeignTableInfo> e : foreignTables.entrySet())
			if (e.getValue().getAnnotation().joinKey().equals(joinKey)) {
				Field field = e.getValue().getField();
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
	
	private String missingForeignTable(String expected, String expectedOn) {
		return "Missing @ForeignTable " + expected + " '" + expectedOn + "' in class '" + type.getSimpleName() + "'";
	}
	
	/** Return the foreign table field having the given join key.
	 * 
	 * @param joinKey join key
	 * @param targetType type of target entity
	 * @return the field and the foreign table annotation
	 */
	public ForeignTableInfo getRequiredForeignTableWithFieldForJoinKey(String joinKey, Class<?> targetType) {
		ForeignTableInfo i = getForeignTableWithFieldForJoinKey(joinKey, targetType);
		if (i == null)
			throw new MappingException(missingForeignTable("field with join key", joinKey));
		return i;
	}
	
	/** Return the foreign table field on the given property.
	 * 
	 * @param propertyName foreign table property
	 * @return the field
	 */
	@Nullable
	public Field getForeignTableFieldForProperty(String propertyName) {
		ForeignTableInfo i = foreignTables.get(propertyName);
		return i != null ? i.getField() : null;
	}
	
	/** Return the foreign table field on the given property.
	 * 
	 * @param propertyName foreign table property
	 * @return the field
	 */
	public Field getRequiredForeignTableFieldForProperty(String propertyName) {
		ForeignTableInfo i = foreignTables.get(propertyName);
		if (i == null)
			throw new MappingException(missingForeignTable("on property", propertyName));
		return i.getField();
	}
	
	/** Return the foreign table field on the given property.
	 * 
	 * @param propertyName foreign table property
	 * @return the foreign table annotation
	 */
	@Nullable
	public ForeignTable getForeignTableForProperty(String propertyName) {
		ForeignTableInfo i = foreignTables.get(propertyName);
		return i != null ? i.getAnnotation() : null;
	}
	
	/** Return the foreign table field on the given property.
	 * 
	 * @param propertyName foreign table property
	 * @return the foreign table annotation
	 */
	public ForeignTable getRequiredForeignTableForProperty(String propertyName) {
		ForeignTableInfo i = foreignTables.get(propertyName);
		if (i == null)
			throw new MappingException(missingForeignTable("on property", propertyName));
		return i.getAnnotation();
	}
	
	/** Return true if the given field is associated to a @ForeignTable annotation.
	 * 
	 * @param field field
	 * @return true if it is a foreign table
	 */
	public static boolean isForeignTableField(Field field) {
		LcEntityTypeInfo ti = cache.get(field.getDeclaringClass());
		if (ti == null)
			return false;
		for (ForeignTableInfo i : ti.foreignTables.values())
			if (i.getField().equals(field))
				return true;
		return false;
	}
	
	/** Return the list of foreign tables.
	 * 
	 * @return list of fields with their corresponding foreign table annotation
	 */
	public Collection<ForeignTableInfo> getForeignTables() {
		return foreignTables.values();
	}
	
	/** Return join table information on the given property name.
	 * 
	 * @param propertyName property
	 * @return join table info
	 */
	@Nullable
	public JoinTableInfo getJoinTable(String propertyName) {
		return joinTables.get(propertyName);
	}
	
	/** Return the list of join tables on this type.
	 * 
	 * @return join tables
	 */
	public Collection<JoinTableInfo> getJoinTables() {
		return joinTables.values();
	}
	
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getJoinTableElementsForJoinTableClass(Object instance, Class<T> joinTableClass) {
		for (JoinTableInfo jti : joinTables.values()) {
			if (ModelUtils.getCollectionType(jti.joinForeignTable.field).equals(joinTableClass)) {
				try {
					return (Collection<T>) jti.joinForeignTable.field.get(instance);
				} catch (Exception e) {
					throw new ModelAccessException("Error accessing join table elements " + joinTableClass.getName() + " from " + instance, e);
				}
			}
		}
		return null;
	}
}
