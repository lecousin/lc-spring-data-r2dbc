package net.lecousin.reactive.data.relational;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import net.lecousin.reactive.data.relational.enhance.Enhancer;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.mapping.LcEntityReader;
import net.lecousin.reactive.data.relational.mapping.LcMappingR2dbcConverter;
import net.lecousin.reactive.data.relational.mapping.LcReactiveDataAccessStrategy;
import net.lecousin.reactive.data.relational.model.EntityCache;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.query.SelectExecution;
import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.query.operation.Operation;
import net.lecousin.reactive.data.relational.schema.RelationalDatabaseSchema;
import net.lecousin.reactive.data.relational.schema.SchemaBuilderFromEntities;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class LcReactiveDataRelationalClient {

	public static final Log logger = LogFactory.getLog(LcReactiveDataRelationalClient.class);
	
	@Autowired
	private MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext;
	
	@Autowired
	private DatabaseClient client;
	
	@Autowired
	private RelationalDatabaseSchemaDialect schemaDialect;
	
	@Autowired
	private LcReactiveDataAccessStrategy dataAccess;
	
	private LcMappingR2dbcConverter mapper;
	
	@PostConstruct
	public void init() {
		// ensure all declared entities have been detected by Spring
		for (Class<?> type : Enhancer.getEntities())
			mappingContext.getPersistentEntity(type);
	}
	
	public LcMappingR2dbcConverter getMapper() {
		return mapper;
	}

	public void setMapper(LcMappingR2dbcConverter mapper) {
		this.mapper = mapper;
	}
	
	public DatabaseClient getSpringClient() {
		return client;
	}
	
	@SuppressWarnings("java:S1452") // usage of generic wildcard type
	public MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> getMappingContext() {
		return mappingContext;
	}
	
	public LcReactiveDataAccessStrategy getDataAccess() {
		return dataAccess;
	}
	
	public RelationalDatabaseSchemaDialect getSchemaDialect() {
		return schemaDialect;
	}
	
	public R2dbcDialect getDialect() {
		return dataAccess.getDialect();
	}

	public Mono<Void> dropSchemaContent(RelationalDatabaseSchema schema) {
		return schemaDialect.dropSchemaContent(schema).execute(this);
	}
	
	public Mono<Void> createSchemaContent(RelationalDatabaseSchema schema) {
		return schemaDialect.createSchemaContent(schema).execute(this);
	}
	
	public Mono<Void> dropCreateSchemaContent(RelationalDatabaseSchema schema) {
		return dropSchemaContent(schema).then(createSchemaContent(schema));
	}
	
	public RelationalDatabaseSchema buildSchemaFromEntities() {
		return new SchemaBuilderFromEntities(this).build(Enhancer.getEntities());
	}
	
	public <T> Mono<T> save(T entity) {
		try {
			@SuppressWarnings("unchecked")
			RelationalPersistentEntity<T> entityType = (RelationalPersistentEntity<T>) mappingContext.getRequiredPersistentEntity(entity.getClass());
			Operation op = new Operation(this);
			op.addToSave(entity, entityType, null, null);
			return op.execute().thenReturn(entity);
		} catch (Exception e) {
			return Mono.error(e);
		}
	}
	
	public <T> Flux<T> save(Iterable<T> entities) {
		try {
			Iterator<T> it = entities.iterator();
			if (!it.hasNext())
				return Flux.empty();
			T instance = it.next();
			@SuppressWarnings("unchecked")
			RelationalPersistentEntity<T> entityType = (RelationalPersistentEntity<T>) mappingContext.getRequiredPersistentEntity(instance.getClass());
			Operation op = new Operation(this);
			op.addToSave(instance, entityType, null, null);
			while (it.hasNext())
				op.addToSave(it.next(), entityType, null, null);
			return op.execute().thenMany(Flux.fromIterable(entities));
		} catch (Exception e) {
			return Flux.error(e);
		}
	}
	
	public <T> Flux<T> save(Publisher<T> publisher) {
		Operation op = new Operation(this);
		List<T> list = new LinkedList<>();
		return Flux.from(publisher)
			.doOnNext(instance -> {
				op.addToSave(instance, null, null, null);
				list.add(instance);
			})
			.then(Mono.fromCallable(op::execute))
			.flatMap(m -> m)
			.thenReturn(list)
			.flatMapMany(Flux::fromIterable);
	}
	
	public <T> Mono<T> lazyLoad(T entity) {
		return lazyLoad(entity, mappingContext.getRequiredPersistentEntity(entity.getClass()));
	}
	
	public <T> Mono<T> lazyLoad(T entity, RelationalPersistentEntity<?> entityType) {
		return lazyLoad(entity, EntityState.get(entity, this, entityType), entityType);
	}
	
	public <T> Mono<T> lazyLoad(T entity, EntityState state, RelationalPersistentEntity<?> entityType) {
		return Mono.fromCallable(() -> state.loading(doLoading(entity, entityType))).flatMap(result -> result);
	}
	
	@SuppressWarnings("unchecked")
	private <T> Mono<T> doLoading(T entity, RelationalPersistentEntity<?> entityType) {
		RelationalPersistentProperty idProperty = entityType.getRequiredIdProperty();
		Object id = ModelUtils.getRequiredId(entity, entityType, null);
		EntityCache cache = new EntityCache();
		cache.setById((Class<T>) entity.getClass(), id, entity);
		return SelectQuery.from((Class<T>) entity.getClass(), "entity")
			.where(Criteria.property("entity", idProperty.getName()).is(id))
			.limit(0, 1)
			.execute(this, new LcEntityReader(cache, getMapper()))
			.next()
			;
	}
	
	public <T> Flux<T> execute(SelectQuery<T> query, @Nullable LcEntityReader reader) {
		return new SelectExecution<T>(query, this, reader).execute();
	}
	
	public <T> Mono<Void> delete(T entity) {
		try {
			@SuppressWarnings("unchecked")
			RelationalPersistentEntity<T> entityType = (RelationalPersistentEntity<T>) mappingContext.getRequiredPersistentEntity(entity.getClass());
			Operation op = new Operation(this);
			op.addToDelete(entity, entityType, null, null);
			return op.execute();
		} catch (Exception e) {
			return Mono.error(e);
		}
	}
	
	public <T> Mono<Void> delete(Iterable<T> entities) {
		try {
			Iterator<T> it = entities.iterator();
			if (!it.hasNext())
				return Mono.empty();
			T instance = it.next();
			@SuppressWarnings("unchecked")
			RelationalPersistentEntity<T> entityType = (RelationalPersistentEntity<T>) mappingContext.getRequiredPersistentEntity(instance.getClass());
			Operation op = new Operation(this);
			op.addToDelete(instance, entityType, null, null);
			while (it.hasNext())
				op.addToDelete(it.next(), entityType, null, null);
			return op.execute();
		} catch (Exception e) {
			return Mono.error(e);
		}
	}
	
	public <T> Mono<Void> delete(Publisher<T> publisher) {
		Operation op = new Operation(this);
		return Flux.from(publisher)
			.doOnNext(instance -> op.addToDelete(instance, null, null, null))
			.then(Mono.fromCallable(op::execute))
			.flatMap(m -> m);
	}
	
}
