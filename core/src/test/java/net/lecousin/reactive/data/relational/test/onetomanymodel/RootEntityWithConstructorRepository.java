package net.lecousin.reactive.data.relational.test.onetomanymodel;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RootEntityWithConstructorRepository extends LcR2dbcRepository<RootEntityWithConstructor, Long> {

	Flux<RootEntity> findByValue(String value);
	
	default Flux<RootEntityWithConstructor> findByValueWithSubEntity(String value) {
		return SelectQuery.from(RootEntityWithConstructor.class, "entity").where(Criteria.property("entity", "value").is(value)).join("entity", "list", "sub").execute(getLcClient());
	}
	
	default Flux<RootEntityWithConstructor> findBySubValue(String value) {
		return SelectQuery.from(RootEntityWithConstructor.class, "entity").join("entity", "list", "sub").where(Criteria.property("sub", "subValue").is(value)).execute(getLcClient());
	}
	
	default Flux<RootEntityWithConstructor> findBySubValueStartsWith(String value, long start, long nb) {
		return SelectQuery.from(RootEntityWithConstructor.class, "entity").join("entity", "list", "sub").where(Criteria.property("sub", "subValue").like(value + '%')).limit(start, nb).execute(getLcClient());
	}

	default Flux<RootEntityWithConstructor> havingSubValueEqualsToValue() {
		return SelectQuery.from(RootEntityWithConstructor.class, "entity").join("entity", "list", "sub").where(Criteria.property("sub", "subValue").is("entity", "value")).execute(getLcClient());
	}
	
	default Flux<RootEntityWithConstructor> findAllFull() {
		return SelectQuery.from(RootEntityWithConstructor.class, "root")
			.join("root", "list", "sub1")
			.execute(getLcClient());
	}
	
	default Mono<RootEntityWithConstructor> getOneWithLinkedEntities(long rootId) {
		return SelectQuery.from(RootEntityWithConstructor.class, "root")
			.where(Criteria.property("root", "id").is(rootId))
			.join("root", "list", "sub1")
			.execute(getLcClient()).next();
	}

}
