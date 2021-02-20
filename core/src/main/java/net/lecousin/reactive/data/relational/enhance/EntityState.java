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
import java.util.function.Supplier;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.CollectionFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.model.LcEntityTypeInfo;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import reactor.core.CorePublisher;
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
	private Map<String, CorePublisher<?>> foreignTablesLoaded = new HashMap<>();
	
	public EntityState(LcReactiveDataRelationalClient client, RelationalPersistentEntity<?> entityType) {
		this.client = client;
		this.entityType = entityType;
	}

	public static EntityState get(Object entity, LcReactiveDataRelationalClient client) {
		return get(entity, client, null);
	}
	
	public static EntityState get(Object entity, LcReactiveDataRelationalClient client, @Nullable RelationalPersistentEntity<?> entityType) {
		try {
			Field fieldInfo = LcEntityTypeInfo.get(entity.getClass()).getStateField();
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
	
	@Nullable
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
	public synchronized <T> Mono<T> loading(Supplier<Mono<T>> doLoading) {
		if (loading != null)
			return (Mono<T>) loading;
		loading = doLoading.get().doOnSuccess(entity -> {
			loading = null;
			loaded(entity);
		}).cache();
		return (Mono<T>) loading;
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public <T> Mono<T> getLoading() {
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
			if (Enhancer.STATE_FIELD_NAME.equals(f.getName()) ||
				f.isAnnotationPresent(Transient.class) ||
				f.isAnnotationPresent(Autowired.class) ||
				f.isAnnotationPresent(Value.class))
				continue;
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
	
	public void restorePersistedValue(Object instance, Field field) {
		field.setAccessible(true);
		Object value = persistedValues.get(field.getName());
		try {
			field.set(instance, value);
		} catch (Exception e) {
			throw new ModelAccessException("Error setting field " + field.getName() + " on " + instance, e);
		}
		modifiedFields.remove(field.getName());
	}
	
	public void setForeignTableField(Object instance, Field field, Object value, boolean saved) {
		setPersistedField(instance, field, value, saved);
		foreignTablesLoaded.put(field.getName(), null);
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
		if (foreignTablesLoaded.containsKey(field.getName()) || (persistedValues.containsKey(field.getName()) && persistedValues.get(field.getName()) != null))
			return new MutableObject<>(null);
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> Mono<T> lazyGetForeignTableField(Object entity, String fieldName, String joinKey) {
		try {
			CorePublisher<?> loading = foreignTablesLoaded.get(fieldName);
			if (loading != null)
				return (Mono<T>)loading;
			MutableObject<T> instance = getForeignTableField(entity, fieldName);
			if (instance != null)
				return instance.getValue() != null ? Mono.just(instance.getValue()) : Mono.empty();
			Field field = entity.getClass().getDeclaredField(fieldName);
			Object id = ModelUtils.getRequiredId(entity, entityType, null);
			RelationalPersistentEntity<?> elementEntity = client.getMappingContext().getRequiredPersistentEntity(field.getType());
			RelationalPersistentProperty fkProperty = elementEntity.getRequiredPersistentProperty(joinKey);
			Mono<T> select = SelectQuery.from((Class<T>) field.getType(), "entity")
				.where(Criteria.property("entity", fkProperty.getName()).is(id))
				.execute(client)
				.next()
				.doOnNext(inst -> {
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
			select = select.cache();
			foreignTablesLoaded.put(fieldName, select);
			return select;
		} catch (Exception e) {
			return Mono.error(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> Flux<T> lazyGetForeignTableCollectionField(Object entity, String fieldName, String joinKey) {
		try {
			CorePublisher<?> loading = foreignTablesLoaded.get(fieldName);
			if (loading != null)
				return (Flux<T>)loading;
			MutableObject<?> instance = getForeignTableField(entity, fieldName);
			if (instance != null) {
				if (instance.getValue() == null)
					return Flux.empty();
				Object collection = instance.getValue();
				if (collection.getClass().isArray())
					return Flux.fromArray((T[]) collection);
				return Flux.fromIterable((Iterable<T>) collection);
			}

			Field field = entity.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			Object id = ModelUtils.getRequiredId(entity, entityType, null);
			Class<?> elementType = ModelUtils.getCollectionType(field);
			if (elementType == null)
				throw new MappingException("Property is not a collection: " + fieldName);
			RelationalPersistentEntity<?> elementEntity = client.getMappingContext().getRequiredPersistentEntity(elementType);
			RelationalPersistentProperty fkProperty = elementEntity.getRequiredPersistentProperty(joinKey);
			Flux<T> flux = SelectQuery.from((Class<T>) elementType, "element")
				.where(Criteria.property("element", fkProperty.getName()).is(id))
				.execute(client);
				
			Field fk = elementType.getDeclaredField(joinKey);
			fk.setAccessible(true);
			if (field.getType().isArray())
				flux = toArray(flux, field, entity, elementType, fk);
			else
				flux = toCollection(flux, field, entity, elementType, fk);
			flux = flux.cache();
			foreignTablesLoaded.put(fieldName, flux);
			return flux;
		} catch (Exception e) {
			return Flux.error(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> Flux<T> lazyGetJoinTableField(Object entity, String joinFieldName, int joinFieldKeyNumber) {
		return lazyGetForeignTableCollectionField(entity, joinFieldName + "_join", "entity" + joinFieldKeyNumber)
			.map(joinEntity -> {
				try {
					Field f = joinEntity.getClass().getDeclaredField("entity" + joinFieldKeyNumber);
					f.setAccessible(true);
					return (T) f.get(joinEntity);
				} catch (Exception e) {
					throw new ModelAccessException("Unable to access to join table property", e);
				}
			});
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
		foreignTablesLoaded.put(field.getName(), null);
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
