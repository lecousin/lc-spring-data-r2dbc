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
package net.lecousin.reactive.data.relational;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.mapping.LcEntityReader;
import net.lecousin.reactive.data.relational.mapping.LcMappingR2dbcConverter;
import net.lecousin.reactive.data.relational.mapping.LcReactiveDataAccessStrategy;
import net.lecousin.reactive.data.relational.model.EntityCache;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.metadata.EntityInstance;
import net.lecousin.reactive.data.relational.model.metadata.EntityMetadata;
import net.lecousin.reactive.data.relational.model.metadata.EntityStaticMetadata;
import net.lecousin.reactive.data.relational.query.SelectExecution;
import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.query.operation.Operation;
import net.lecousin.reactive.data.relational.schema.RelationalDatabaseSchema;
import net.lecousin.reactive.data.relational.schema.SchemaBuilderFromEntities;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Reactive client for a relational database connection handling features such as join, lazy loading, persistence, deletion...
 * 
 * @author Guillaume Le Cousin
 *
 */
@Component
public class LcReactiveDataRelationalClient {

	public static final Log logger = LogFactory.getLog(LcReactiveDataRelationalClient.class);
	
	private static final String QUERY_ENTITY_NAME = "entity";
	
	private DatabaseClient client;
	private RelationalDatabaseSchemaDialect schemaDialect;
	private LcReactiveDataAccessStrategy dataAccess;
	private LcMappingR2dbcConverter mapper;
	private Map<Class<?>, EntityMetadata> entities;
	
	public LcReactiveDataRelationalClient(
		DatabaseClient client,
		MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext,
		RelationalDatabaseSchemaDialect schemaDialect,
		LcReactiveDataAccessStrategy dataAccess,
		LcMappingR2dbcConverter mapper
	) {
		this.client = client;
		this.schemaDialect = schemaDialect;
		this.dataAccess = dataAccess;
		this.mapper = mapper;
		this.mapper.setLcClient(this);
		// ensure all declared entities have been detected by Spring, and load them
		this.entities = new HashMap<>();
		for (Class<?> type : EntityStaticMetadata.addGeneratedJoinTables(EntityStaticMetadata.getClasses())) {
			RelationalPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(type);
			this.entities.put(entity.getType(), new EntityMetadata(this, entity));
		}
	}
	
	/** @return the Spring R2DBC database client. */
	public DatabaseClient getSpringClient() {
		return client;
	}
	
	/** @return entity mapper and converters. */
	public LcMappingR2dbcConverter getMapper() {
		return mapper;
	}
	
	/** @return the Spring Data R2DBC data access strategy. */
	public LcReactiveDataAccessStrategy getDataAccess() {
		return dataAccess;
	}
	
	/** @return the extended dialect. */
	public RelationalDatabaseSchemaDialect getSchemaDialect() {
		return schemaDialect;
	}
	
	/** @return the R2DBC native dialect. */
	public R2dbcDialect getDialect() {
		return dataAccess.getDialect();
	}
	
	public @NonNull EntityMetadata getRequiredEntity(Class<?> type) {
		EntityMetadata entity = entities.get(type);
		if (entity == null)
			throw new ModelAccessException("Unknown entity type: " + type.getName());
		return entity;
	}
	
	public Collection<EntityMetadata> getEntities() {
		return entities.values();
	}
	
	public Collection<EntityMetadata> getEntities(Collection<Class<?>> types) {
		ArrayList<EntityMetadata> list = new ArrayList<>(types.size());
		for (Class<?> type : types)
			list.add(getRequiredEntity(type));
		return list;
	}

	/** Drop all elements from the given schema. */
	public Mono<Void> dropSchemaContent(RelationalDatabaseSchema schema) {
		return schemaDialect.dropSchemaContent(schema).execute(this);
	}
	
	/** Create tables, constraints and sequences from the given schema. */
	public Mono<Void> createSchemaContent(RelationalDatabaseSchema schema) {
		return schemaDialect.createSchemaContent(schema).execute(this);
	}
	
	/** Drop then create the given schema. */
	public Mono<Void> dropCreateSchemaContent(RelationalDatabaseSchema schema) {
		return dropSchemaContent(schema).then(createSchemaContent(schema));
	}
	
	/** Build the schema definition from all known entities. */
	public RelationalDatabaseSchema buildSchemaFromEntities() {
		return buildSchemaFromEntities(entities.keySet());
	}
	
	/** Build the schema definition from given entities. */
	public RelationalDatabaseSchema buildSchemaFromEntities(Collection<Class<?>> classes) {
		return SchemaBuilderFromEntities.build(getEntities(EntityStaticMetadata.addGeneratedJoinTables(classes)));
	}
	
	/** Save the given entity (insert or update in cascade). */
	public <T> Mono<T> save(T entity) {
		try {
			EntityInstance<T> instance = getInstance(entity);
			Operation op = new Operation(this);
			op.addToSave(instance);
			return op.execute().thenReturn(entity);
		} catch (Exception e) {
			return Mono.error(e);
		}
	}
	
	/** Save the given entities (insert or update in cascade). */
	public <T> Flux<T> save(Iterable<T> entities) {
		try {
			Iterator<T> it = entities.iterator();
			if (!it.hasNext())
				return Flux.empty();
			T entity = it.next();
			EntityInstance<T> instance = getInstance(entity);
			Operation op = new Operation(this);
			op.addToSave(instance);
			while (it.hasNext())
				op.addToSave(getInstance(it.next()));
			return op.execute().thenMany(Flux.fromIterable(entities));
		} catch (Exception e) {
			return Flux.error(e);
		}
	}
	
	/** Save the given entities (insert or update in cascade). */
	public <T> Flux<T> save(Publisher<T> publisher) {
		Operation op = new Operation(this);
		List<T> list = new LinkedList<>();
		return Flux.from(publisher)
			.doOnNext(instance -> {
				op.addToSave(getInstance(instance));
				list.add(instance);
			})
			.then(Mono.fromCallable(op::execute))
			.flatMap(m -> m)
			.thenReturn(list)
			.flatMapMany(Flux::fromIterable);
	}
	
	/** Save the given entities (insert or update in cascade). */
	public Mono<Void> saveAll(Iterable<Object> entities) {
		Iterator<Object> it = entities.iterator();
		if (!it.hasNext())
			return Mono.empty();
		Operation op = new Operation(this);
		do {
			op.addToSave(getInstance(it.next()));
		} while (it.hasNext());
		return op.execute();
	}
	
	/** Save the given entities (insert or update in cascade). */
	public Mono<Void> saveAll(Object... entities) {
		return saveAll(Arrays.asList(entities));
	}
	
	public <T> EntityInstance<T> getInstance(T entity) {
		return new EntityInstance<>(entity, EntityState.get(entity, this));
	}
	
	/** Load the given entity from database. */
	public <T> Mono<T> lazyLoad(T entity) {
		return lazyLoadInstance(getInstance(entity)).map(EntityInstance::getEntity);
	}
	
	/** Load the given entity from database. */
	public <T> Mono<EntityInstance<T>> lazyLoadInstance(EntityInstance<T> instance) {
		return Mono.fromCallable(() -> instance.getState().loading(instance, () -> doLoading(instance))).flatMap(result -> result);
	}
	
	@SuppressWarnings("unchecked")
	private <T> Mono<EntityInstance<T>> doLoading(EntityInstance<T> instance) {
		Class<T> type = (Class<T>) instance.getEntity().getClass();
		Object id = instance.getId();
		EntityCache cache = new EntityCache();
		cache.setInstanceById(id, instance);
		return SelectQuery.from(type, QUERY_ENTITY_NAME)
			.where(instance.getCriteriaOnId(QUERY_ENTITY_NAME))
			.limit(0, 1)
			.execute(this, new LcEntityReader(cache, getMapper()))
			.next()
			.map(read -> instance)
			;
	}

	/** Load the given entities from database. */
	public <T> Flux<EntityInstance<T>> lazyLoadInstances(Iterable<EntityInstance<T>> entities) {
		List<Mono<EntityInstance<T>>> alreadyLoading = new LinkedList<>();
		List<EntityInstance<T>> toLoad = new LinkedList<>();
		for (EntityInstance<T> entity : entities) {
			Mono<T> loading = entity.getState().getLoading();
			if (loading != null)
				alreadyLoading.add(loading.map(e -> entity));
			else
				toLoad.add(entity);
		}
		Flux<EntityInstance<T>> loading = doLoading(toLoad).cache();
		for (EntityInstance<T> entity : toLoad) {
			alreadyLoading.add(entity.getState().loading(entity, () -> loading.filter(e -> e.getEntity() == entity.getEntity()).next()));
		}
		return Flux.merge(alreadyLoading);
	}
	
	@SuppressWarnings("unchecked")
	private <T> Flux<EntityInstance<T>> doLoading(Iterable<EntityInstance<T>> entities) {
		Iterator<EntityInstance<T>> it = entities.iterator();
		if (!it.hasNext())
			return Flux.empty();
		EntityInstance<T> instance = it.next();
		if (!it.hasNext())
			return Flux.from(doLoading(instance));
		Class<T> type = (Class<T>) instance.getEntity().getClass();
		EntityCache cache = new EntityCache();
		Criteria criteria = null;
		do {
			Object id = instance.getId();
			cache.setInstanceById(id, instance);
			Criteria entityCriteria = instance.getCriteriaOnId(QUERY_ENTITY_NAME);
			criteria = criteria != null ? criteria.or(entityCriteria) : entityCriteria;
			if (!it.hasNext())
				break;
			instance = it.next();
		} while (true);
		return SelectQuery.from(type, QUERY_ENTITY_NAME)
			.where(criteria)
			.execute(this, new LcEntityReader(cache, getMapper()))
			.map(cache::getInstance)
			;
	}
	
	/** Execute a select query using the given LcEntityReader to map rows to entities. */
	public <T> Flux<T> execute(SelectQuery<T> query, @Nullable LcEntityReader reader) {
		return new SelectExecution<T>(query, this, reader).execute();
	}
	
	/** Execute a select query. */
	public Mono<Long> executeCount(SelectQuery<?> query) {
		return new SelectExecution<>(query, this, null).executeCount();
	}
	
	/** Delete the given entity (with cascade). */
	public <T> Mono<Void> delete(T entity) {
		try {
			EntityInstance<T> instance = getInstance(entity);
			Operation op = new Operation(this);
			op.addToDelete(instance);
			return op.execute();
		} catch (Exception e) {
			return Mono.error(e);
		}
	}
	
	/** Delete the given entities (with cascade). */
	public <T> Mono<Void> delete(Iterable<T> entities) {
		try {
			Iterator<T> it = entities.iterator();
			if (!it.hasNext())
				return Mono.empty();
			T entity = it.next();
			Operation op = new Operation(this);
			op.addToDelete(getInstance(entity));
			while (it.hasNext())
				op.addToDelete(getInstance(it.next()));
			return op.execute();
		} catch (Exception e) {
			return Mono.error(e);
		}
	}

	/** Delete the given entities (with cascade). */
	public <T> Mono<Void> delete(Publisher<T> publisher) {
		return delete(publisher, 100, Duration.ofSeconds(1));
	}
	
	/** Delete the given entities (with cascade), by bunch.
	 * @param publisher entities to delete
	 * @param bunchSize bufferize entities to delete them by bunch
	 * @param bunchTimeout timeout after which a the current bunch of entities are deleted even the bunch is not full
	 */
	public <T> Mono<Void> delete(Publisher<T> publisher, int bunchSize, Duration bunchTimeout) {
		return Flux.from(publisher)
			.subscribeOn(Schedulers.parallel()).publishOn(Schedulers.parallel())
			.bufferTimeout(bunchSize, bunchTimeout)
			.parallel()
			.runOn(Schedulers.parallel(), 1)
			.flatMap(this::delete)
			.then();
	}
	
}
