package net.lecousin.reactive.data.relational.test.onetoonemodel;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Flux;

public interface MyEntity1WithConstructorRepository extends LcR2dbcRepository<MyEntity1WithConstructor, Long> {
	
	default Flux<MyEntity1WithConstructor> findByValue(String value) {
		return SelectQuery.from(MyEntity1WithConstructor.class, "entity").where(Criteria.property("entity", "value").is(value)).join("entity", "subEntity", "sub").execute(getLcClient());
	}
	
}
