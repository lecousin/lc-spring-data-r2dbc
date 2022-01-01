package net.lecousin.reactive.data.relational.repository;

import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.repository.support.SimpleR2dbcRepository;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.repository.query.RelationalEntityInformation;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@SuppressWarnings("java:S119") // name of parameter ID
public class LcR2dbcRepositoryImpl<T, ID> extends SimpleR2dbcRepository<T, ID> implements LcR2dbcRepository<T, ID> {

	private LcReactiveDataRelationalClient lcClient;
	private RelationalEntityInformation<T, ID> entityInfo;
	private R2dbcEntityOperations entityOperations;
	
	public LcR2dbcRepositoryImpl(RelationalEntityInformation<T, ID> entity, R2dbcEntityOperations entityOperations, R2dbcConverter converter) {
		super(entity, entityOperations, converter);
		lcClient = ((LcR2dbcEntityTemplate)entityOperations).getLcClient();
		this.entityInfo = entity;
		this.entityOperations = entityOperations;
	}

	@Override
	public LcReactiveDataRelationalClient getLcClient() {
		return lcClient;
	}
	
	@Override
	public <S extends T> Mono<S> save(S objectToSave) {
		return lcClient.save(objectToSave);
	}
	
	@Override
	public <S extends T> Flux<S> saveAll(Iterable<S> objectsToSave) {
		return lcClient.save(objectsToSave);
	}
	
	@Override
	public <S extends T> Flux<S> saveAll(Publisher<S> objectsToSave) {
		return lcClient.save(objectsToSave);
	}
	
	@Override
	public Mono<Void> delete(T objectToDelete) {
		return lcClient.delete(objectToDelete);
	}
	
	@Override
	public Mono<Void> deleteAll(Iterable<? extends T> iterable) {
		return lcClient.delete(iterable);
	}
	
	@Override
	public Mono<Void> deleteAll(Publisher<? extends T> objectPublisher) {
		return lcClient.delete(objectPublisher);
	}
	
	@Override
	public Mono<Void> deleteAll() {
		if (ModelUtils.hasCascadeDeleteImpacts(entityInfo.getJavaType(), lcClient.getMappingContext()))
			return deleteAll(findAll());
		return entityOperations.delete(entityInfo.getJavaType()).all().then();
	}
	
	@Override
	public Mono<Void> deleteById(ID id) {
		if (ModelUtils.hasCascadeDeleteImpacts(entityInfo.getJavaType(), lcClient.getMappingContext()))
			return findById(id).flatMap(this::delete);
		RelationalPersistentEntity<?> entity = lcClient.getMappingContext().getRequiredPersistentEntity(entityInfo.getJavaType());
		if (!entity.hasIdProperty())
			return findById(id).flatMap(this::delete);
		return entityOperations.delete(entityInfo.getJavaType()).matching(Query.query(Criteria.where(entity.getRequiredIdProperty().getColumnName().getReference()).is(id))).all().then();
	}
	
	@Override
	public Mono<Void> deleteById(Publisher<ID> idPublisher) {
		if (ModelUtils.hasCascadeDeleteImpacts(entityInfo.getJavaType(), lcClient.getMappingContext()))
			return deleteAll(findAllById(idPublisher));
		RelationalPersistentEntity<?> entity = lcClient.getMappingContext().getRequiredPersistentEntity(entityInfo.getJavaType());
		if (!entity.hasIdProperty())
			return deleteAll(findAllById(idPublisher));
		return Flux.from(idPublisher)
			.subscribeOn(Schedulers.parallel()).publishOn(Schedulers.parallel())
			.buffer(100)
			.parallel()
			.runOn(Schedulers.parallel(), 1)
			.flatMap(ids -> entityOperations.delete(entityInfo.getJavaType()).matching(Query.query(Criteria.where(entity.getRequiredIdProperty().getColumnName().getReference()).in(ids))).all().then())
			.then();
	}
	
}
