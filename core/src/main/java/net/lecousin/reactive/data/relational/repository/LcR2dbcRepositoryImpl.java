package net.lecousin.reactive.data.relational.repository;

import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.repository.support.SimpleR2dbcRepository;
import org.springframework.data.relational.repository.query.RelationalEntityInformation;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SuppressWarnings("java:S119") // name of parameter ID
public class LcR2dbcRepositoryImpl<T, ID> extends SimpleR2dbcRepository<T, ID> implements LcR2dbcRepository<T, ID> {

	private LcReactiveDataRelationalClient lcClient;
	
	public LcR2dbcRepositoryImpl(RelationalEntityInformation<T, ID> entity, R2dbcEntityOperations entityOperations, R2dbcConverter converter) {
		super(entity, entityOperations, converter);
	}

	public LcR2dbcRepositoryImpl(RelationalEntityInformation<T, ID> entity, DatabaseClient databaseClient, R2dbcConverter converter, ReactiveDataAccessStrategy accessStrategy) {
		super(entity, databaseClient, converter, accessStrategy);
	}
	
	void setLcClient(LcReactiveDataRelationalClient lcClient) {
		this.lcClient = lcClient;
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
		// TODO we may not need to load them
		return deleteAll(findAll());
	}
	
	@Override
	public Mono<Void> deleteById(ID id) {
		// TODO we may not need to load it
		return findById(id).flatMap(this::delete);
	}
	
	@Override
	public Mono<Void> deleteById(Publisher<ID> idPublisher) {
		// TODO we may not need to load them
		return deleteAll(findById(idPublisher));
	}
	
}
