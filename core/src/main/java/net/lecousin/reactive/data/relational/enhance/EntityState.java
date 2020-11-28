package net.lecousin.reactive.data.relational.enhance;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.core.CollectionFactory;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SuppressWarnings({"java:S3011"})
public class EntityState {

	private LcReactiveDataRelationalClient client;
	private RelationalPersistentEntity<?> entityType;
	private boolean persisted = false;
	private boolean loaded = false;
	private Mono<?> loading = null;
	private Map<String, Object> persistedValues = new HashMap<>();
	private Set<String> modifiedFields = new HashSet<>();
	private Set<String> foreignTablesLoaded = new HashSet<>();
	
	public EntityState(LcReactiveDataRelationalClient client, RelationalPersistentEntity<?> entityType) {
		this.client = client;
		this.entityType = entityType;
	}

	public static EntityState get(Object entity, LcReactiveDataRelationalClient client) {
		return get(entity, client, null);
	}
	
	public static EntityState get(Object entity, LcReactiveDataRelationalClient client, @Nullable RelationalPersistentEntity<?> entityType) {
		try {
			Field fieldInfo = Enhancer.entities.get(entity.getClass());
			EntityState state = (EntityState) fieldInfo.get(entity);
			if (state == null) {
				RelationalPersistentEntity<?> type;
				if (entityType == null)
					type = client.getMappingContext().getRequiredPersistentEntity(entity.getClass());
				else
					type = entityType;
				state = new EntityState(client, type);
				fieldInfo.set(entity, state);
			}
			return state;
		} catch (Exception e) {
			throw new ModelAccessException("Unexpected error accessing entity state for " + entity, e);
		}
	}
	
	public boolean isPersisted() {
		return persisted;
	}
	
	public boolean isFieldModified(String name) {
		return modifiedFields.contains(name);
	}
	
	public Object getPersistedValue(String fieldName) {
		return persistedValues.get(fieldName);
	}
	
	public void deleted() {
		persisted = false;
		loaded = false;
		loading = null;
		persistedValues.clear();
		modifiedFields.clear();
		foreignTablesLoaded.clear();
	}
	
	public void lazyLoaded() {
		persisted = true;
		loaded = false;
	}
	
	public boolean isLoaded() {
		return loaded;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized <T> Mono<T> loading(Mono<T> doLoading) {
		if (loading != null)
			return (Mono<T>) loading;
		loading = doLoading.doOnSuccess(entity -> {
			loading = null;
			loaded(entity);
		}).cache();
		return (Mono<T>) loading;
	}
	
	public <T> void loaded(T entity) {
		persisted = true;
		loaded = true;
		updatePersistedValues(entity);
	}
	
	private void updatePersistedValues(Object entity) {
		modifiedFields.clear();
		persistedValues.clear();
		for (Field f : entity.getClass().getDeclaredFields()) {
			if (Enhancer.STATE_FIELD_NAME.equals(f.getName()))
				continue;
			// TODO exclude transient fields
			f.setAccessible(true);
			try {
				savePersistedValue(f, f.get(entity));
			} catch (Exception e) {
				throw new ModelAccessException("Error saving value for field " + f.getName(), e);
			}
		}
	}
	
	private void savePersistedValue(Field field, Object value) {
		if (value != null && ModelUtils.isCollection(field)) {
			List<Object> list = new LinkedList<>();
			list.addAll(ModelUtils.getAsCollection(value));
			persistedValues.put(field.getName(), list);
		} else {
			persistedValues.put(field.getName(), value);
		}
	}
	
	public <T> Mono<T> load(T entity) {
		return client.lazyLoad(entity, this, entityType);
	}
	
	public void fieldSet(String fieldName, Object newValue) {
		if (Objects.equals(newValue, persistedValues.get(fieldName))) {
			modifiedFields.remove(fieldName);
			return;
		}
		modifiedFields.add(fieldName);
	}
	
	public void fieldSet(String fieldName, boolean newValue) {
		fieldSet(fieldName, Boolean.valueOf(newValue));
	}
	
	public void fieldSet(String fieldName, byte newValue) {
		fieldSet(fieldName, Byte.valueOf(newValue));
	}
	
	public void fieldSet(String fieldName, short newValue) {
		fieldSet(fieldName, Short.valueOf(newValue));
	}
	
	public void fieldSet(String fieldName, int newValue) {
		fieldSet(fieldName, Integer.valueOf(newValue));
	}
	
	public void fieldSet(String fieldName, long newValue) {
		fieldSet(fieldName, Long.valueOf(newValue));
	}
	
	public void fieldSet(String fieldName, float newValue) {
		fieldSet(fieldName, Float.valueOf(newValue));
	}
	
	public void fieldSet(String fieldName, double newValue) {
		fieldSet(fieldName, Double.valueOf(newValue));
	}
	
	public void fieldSet(String fieldName, char newValue) {
		fieldSet(fieldName, Character.valueOf(newValue));
	}
	
	public void setPersistedField(Object instance, Field field, Object value, boolean saved) {
		field.setAccessible(true);
		try {
			field.set(instance, value);
		} catch (Exception e) {
			throw new ModelAccessException("Error setting field " + field.getName() + " on " + instance, e);
		}
		if (Objects.equals(value, persistedValues.get(field.getName()))) {
			modifiedFields.remove(field.getName());
		} else if (saved) {
			modifiedFields.remove(field.getName());
			savePersistedValue(field, value);
		} else {
			modifiedFields.add(field.getName());
		}
	}
	
	public void setForeignTableField(Object instance, Field field, Object value, boolean saved) {
		setPersistedField(instance, field, value, saved);
		foreignTablesLoaded.add(field.getName());
	}
	
	@Nullable
	public <T> MutableObject<T> getForeignTableField(Object entity, String fieldName) throws IllegalAccessException, NoSuchFieldException {
		return getForeignTableField(entity, entity.getClass().getDeclaredField(fieldName));
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public <T> MutableObject<T> getForeignTableField(Object entity, Field field) throws IllegalAccessException {
		field.setAccessible(true);
		Object instance = field.get(entity);
		if (instance != null)
			return new MutableObject<>((T) instance);
		if (foreignTablesLoaded.contains(field.getName()) || (persistedValues.containsKey(field.getName()) && persistedValues.get(field.getName()) != null))
			return new MutableObject<>(null);
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> Mono<T> lazyGetForeignTableField(Object entity, String fieldName, String joinKey) {
		try {
			MutableObject<T> instance = getForeignTableField(entity, fieldName);
			if (instance != null)
				return instance.getValue() != null ? Mono.just(instance.getValue()) : Mono.empty();
			foreignTablesLoaded.add(fieldName);
			Field field = entity.getClass().getDeclaredField(fieldName);
			Object id = ModelUtils.getRequiredId(entity, entityType, null);
			RelationalPersistentEntity<?> elementEntity = client.getMappingContext().getRequiredPersistentEntity(field.getType());
			RelationalPersistentProperty fkProperty = elementEntity.getRequiredPersistentProperty(joinKey);
			Mono<T> fromDb = (Mono<T>) client.getSpringClient().select().from(field.getType()).matching(Criteria.where(client.getDataAccess().toSql(fkProperty.getColumnName())).is(id)).fetch().one();
			fromDb = fromDb.doOnNext(inst -> {
				try {
					field.setAccessible(true);
					field.set(entity, inst);
					savePersistedValue(field, inst);
					Field fk = field.getType().getDeclaredField(joinKey);
					fk.setAccessible(true);
					fk.set(inst, entity);
				} catch (Exception e) {
					throw new ModelAccessException("Unable to set " + fieldName, e);
				}
			});
			return fromDb;
		} catch (Exception e) {
			return Mono.error(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> Flux<T> lazyGetForeignTableCollectionField(Object entity, String fieldName, String joinKey) {
		try {
			MutableObject<?> instance = getForeignTableField(entity, fieldName);
			if (instance != null) {
				if (instance.getValue() == null)
					return Flux.empty();
				Object collection = instance.getValue();
				if (collection.getClass().isArray())
					return Flux.fromArray((T[]) collection);
				return Flux.fromIterable((Iterable<T>) collection);
			}

			foreignTablesLoaded.add(fieldName);
			Field field = entity.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			Object id = ModelUtils.getRequiredId(entity, entityType, null);
			Class<?> elementType = ModelUtils.getCollectionType(field);
			if (elementType == null)
				throw new MappingException("Property is not a collection: " + fieldName);
			RelationalPersistentEntity<?> elementEntity = client.getMappingContext().getRequiredPersistentEntity(elementType);
			RelationalPersistentProperty fkProperty = elementEntity.getRequiredPersistentProperty(joinKey);
			Flux<T> flux = (Flux<T>) client.getSpringClient().select().from(elementType).matching(Criteria.where(client.getDataAccess().toSql(fkProperty.getColumnName())).is(id)).fetch().all();
			Field fk = elementType.getDeclaredField(joinKey);
			fk.setAccessible(true);
			if (field.getType().isArray())
				return toArray(flux, field, entity, elementType, fk);
			return toCollection(flux, field, entity, elementType, fk);
		} catch (Exception e) {
			return Flux.error(e);
		}
	}
	
	private static <T> Flux<T> toArray(Flux<T> flux, Field field, Object entity, Class<?> elementType, Field fk) {
		return flux.collectList().flatMapMany(list -> {
			Object array = list.toArray((Object[])Array.newInstance(elementType, list.size()));
			try {
				field.set(entity, array);
			} catch (Exception e) {
				return Flux.error(e);
			}
			for (Object element : list)
				try {
					fk.set(element, entity);
				} catch (Exception e) {
					throw new ModelAccessException("Unable to set field " + fk.getName(), e);
				}
			return Flux.fromIterable(list);
		});
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> Flux<T> toCollection(Flux<T> flux, Field field, Object entity, Class<?> elementType, Field fk) throws IllegalAccessException {
		final Object col = CollectionFactory.createCollection(field.getType(), elementType, 10);
		field.set(entity, col);
		flux = flux.doOnNext(element -> {
			((Collection)col).add(element);
			try {
				fk.set(element, entity);
			} catch (Exception e) {
				throw new ModelAccessException("Unable to set field " + fk.getName(), e);
			}
		});
		flux = flux.doOnComplete(() -> savePersistedValue(field, col));
		return flux;
	}
	
	public void foreignTableLoaded(Field field, Object value) {
		foreignTablesLoaded.add(field.getName());
		savePersistedValue(field, value);
	}
	
	@SuppressWarnings("unchecked")
	public <T, R> Function<T, R> getFieldMapper(Object entity, String fieldName) {
		try {
			Field field = entity.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return e -> {
				try {
					return (R) field.get(e);
				} catch (Exception err) {
					throw new ModelAccessException("Unable to access field " + fieldName, err);
				}
			};
		} catch (Exception e) {
			throw new ModelAccessException("Unable to access field " + fieldName, e);
		}
	}
}
