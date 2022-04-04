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
package net.lecousin.reactive.data.relational.enhance;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.core.CollectionFactory;
import org.springframework.data.mapping.MappingException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.model.metadata.EntityInstance;
import net.lecousin.reactive.data.relational.model.metadata.EntityMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyStaticMetadata;
import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import reactor.core.CorePublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Internal state of an entity, allowing to implement features such as lazy loading, updated attributes detection...
 * 
 * @author Guillaume Le Cousin
 *
 */
public class EntityState {

	private EntityMetadata entityType;
	private boolean persisted = false;
	private boolean loaded = false;
	private Mono<?> loading = null;
	private Map<String, Object> persistedValues = new HashMap<>();
	private Map<String, CorePublisher<?>> foreignTablesLoaded = new HashMap<>();
	
	private static final String ENTITY_ALIAS = "entity";
	
	public EntityState(@NonNull EntityMetadata entityType) {
		this.entityType = entityType;
	}

	public static EntityState get(@NonNull Object entity, @NonNull LcReactiveDataRelationalClient client) {
		return get(entity, client.getRequiredEntity(entity.getClass()));
	}
	
	@SuppressWarnings("java:S3011")
	public static EntityState get(@NonNull Object entity, @NonNull EntityMetadata entityType) {
		try {
			Field fieldInfo = entityType.getStaticMetadata().getStateField();
			EntityState state = (EntityState) fieldInfo.get(entity);
			if (state == null) {
				state = new EntityState(entityType);
				fieldInfo.set(entity, state);
			}
			return state;
		} catch (Exception e) {
			throw new ModelAccessException("Unexpected error accessing entity state for " + entity, e);
		}
	}
	
	public EntityMetadata getMetadata() {
		return entityType;
	}
	
	public LcReactiveDataRelationalClient getClient() {
		return entityType.getClient();
	}
	
	public boolean isPersisted() {
		return persisted;
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
	public synchronized <T> Mono<EntityInstance<T>> loading(EntityInstance<T> instance, Supplier<Mono<EntityInstance<T>>> doLoading) {
		if (loading != null)
			return (Mono<EntityInstance<T>>) loading;
		if (loaded)
			return Mono.just(instance);
		loading = doLoading.get().doOnSuccess(entity -> {
			if (entity == null)
				deleted();
			else
				loaded(entity.getEntity());
			loading = null;
		}).cache();
		return (Mono<EntityInstance<T>>) loading;
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
		persistedValues.clear();
		for (PropertyMetadata property : entityType.getProperties()) {
			Field f = property.getStaticMetadata().getField();
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
		return entityType.getClient().lazyLoad(entity);
	}
	
	public void restorePersistedValue(Object instance, PropertyStaticMetadata property) {
		Field field = property.getField();
		ModelUtils.setFieldValue(instance, field, persistedValues.get(field.getName()));
	}
	
	public void setForeignTableField(Object instance, PropertyStaticMetadata property, Object value) {
		Field field = property.getField();
		ModelUtils.setFieldValue(instance, field, value);
		foreignTablesLoaded.put(field.getName(), null);
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public <T> MutableObject<T> getForeignTableField(Object entity, PropertyStaticMetadata property) throws IllegalAccessException {
		Field field = property.getField();
		Object instance = field.get(entity);
		if (instance != null)
			return new MutableObject<>((T) instance);
		String name = field.getName();
		if (foreignTablesLoaded.containsKey(name) || (persistedValues.containsKey(name) && persistedValues.get(name) != null))
			return new MutableObject<>(null);
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> Mono<T> lazyGetForeignTableField(Object entity, String fieldName, String joinKey) {
		try {
			CorePublisher<?> foreignLoading = foreignTablesLoaded.get(fieldName);
			if (foreignLoading != null)
				return (Mono<T>)foreignLoading;
			PropertyMetadata property = entityType.getRequiredProperty(fieldName);
			MutableObject<T> instance = getForeignTableField(entity, property.getStaticMetadata());
			if (instance != null)
				return instance.getValue() != null ? Mono.just(instance.getValue()) : Mono.empty();
			Field field = property.getStaticMetadata().getField();
			EntityInstance<?> entityInstance = new EntityInstance<>(entity, this);
			Object id = entityInstance.getId();
			LcReactiveDataRelationalClient client = getClient();
			PropertyMetadata fkProperty = client.getRequiredEntity(field.getType()).getRequiredProperty(joinKey);
			Mono<T> select = SelectQuery.from((Class<T>) field.getType(), ENTITY_ALIAS)
				.where(Criteria.property(ENTITY_ALIAS, fkProperty.getName()).is(id))
				.execute(client)
				.next()
				.doOnNext(inst -> {
					ModelUtils.setFieldValue(entity, field, inst);
					savePersistedValue(field, inst);
					ModelUtils.setFieldValue(inst, fkProperty.getStaticMetadata().getField(), entity);
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
			CorePublisher<?> foreignLoading = foreignTablesLoaded.get(fieldName);
			if (foreignLoading != null)
				return (Flux<T>)foreignLoading;
			PropertyMetadata property = entityType.getRequiredProperty(fieldName);
			MutableObject<?> instance = getForeignTableField(entity, property.getStaticMetadata());
			if (instance != null) {
				if (instance.getValue() == null)
					return Flux.empty();
				Object collection = instance.getValue();
				if (collection.getClass().isArray())
					return Flux.fromArray((T[]) collection);
				return Flux.fromIterable((Iterable<T>) collection);
			}

			Field field = property.getStaticMetadata().getField();
			EntityInstance<?> entityInstance = new EntityInstance<>(entity, this);
			Object id = entityInstance.getId();
			Class<?> elementType = ModelUtils.getCollectionType(field);
			if (elementType == null)
				throw new MappingException("Property is not a collection: " + fieldName);
			LcReactiveDataRelationalClient client = getClient();
			PropertyMetadata fkProperty = client.getRequiredEntity(elementType).getRequiredProperty(joinKey);
			Flux<T> flux = SelectQuery.from((Class<T>) elementType, "element")
				.where(Criteria.property("element", fkProperty.getName()).is(id))
				.execute(client);
				
			Field fk = fkProperty.getStaticMetadata().getField();
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
	
	@SuppressWarnings({"unchecked", "java:S3011"})
	public <T> Flux<T> lazyGetJoinTableField(Object entity, String joinFieldName, int joinFieldKeyNumber) {
		return lazyGetForeignTableCollectionField(entity, joinFieldName + "_join", Enhancer.JOIN_TABLE_ATTRIBUTE_PREFIX + joinFieldKeyNumber)
			.map(joinEntity -> {
				try {
					Field f = joinEntity.getClass().getDeclaredField(Enhancer.JOIN_TABLE_ATTRIBUTE_PREFIX + joinFieldKeyNumber);
					f.setAccessible(true);
					return (T) f.get(joinEntity);
				} catch (Exception e) {
					throw new ModelAccessException("Unable to access to join table property", e);
				}
			});
	}
	
	@SuppressWarnings("java:S3011")
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
	
	@SuppressWarnings({ "unchecked", "rawtypes", "java:S3011" })
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
	public <T, R> Function<T, R> getFieldMapper(String fieldName) {
		try {
			PropertyMetadata property = entityType.getRequiredProperty(fieldName);
			return e -> (R) ModelUtils.getFieldValue(e, property.getStaticMetadata().getField());
		} catch (Exception e) {
			throw new ModelAccessException("Unable to access field " + fieldName, e);
		}
	}
}
