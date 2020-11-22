package net.lecousin.reactive.data.relational.test.onetomanymodel;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Flux;

public interface RootEntityRepository extends LcR2dbcRepository<RootEntity, Long> {

	default Flux<RootEntity> findByValue(String value) {
		return getLcClient().execute(SelectQuery.from(RootEntity.class, "entity").where(Criteria.property("entity", "value").is(value)).join("entity", "list", "sub"));
	}
	
	default Flux<RootEntity> findBySubValue(String value) {
		return getLcClient().execute(SelectQuery.from(RootEntity.class, "entity").join("entity", "list", "sub").where(Criteria.property("sub", "subValue").is(value)));
	}
	
	default Flux<RootEntity> findBySubValueStartsWith(String value, long start, long nb) {
		return getLcClient().execute(SelectQuery.from(RootEntity.class, "entity").join("entity", "list", "sub").where(Criteria.property("sub", "subValue").like(value + '%')).limit(start, nb));
	}

	default Flux<RootEntity> havingSubValueEqualsToValue() {
		return getLcClient().execute(SelectQuery.from(RootEntity.class, "entity").join("entity", "list", "sub").where(Criteria.property("sub", "subValue").is("entity", "value")));
	}
	
	default Flux<RootEntity> findAllFull() {
		return getLcClient().execute(
			SelectQuery.from(RootEntity.class, "root")
			.join("root", "list", "sub1")
			.join("root", "list2", "sub2")
			.join("root", "list3", "sub3")
		);
	}

}
