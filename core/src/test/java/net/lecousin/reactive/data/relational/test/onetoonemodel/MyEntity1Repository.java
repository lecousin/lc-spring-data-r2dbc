package net.lecousin.reactive.data.relational.test.onetoonemodel;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Flux;

public interface MyEntity1Repository extends LcR2dbcRepository<MyEntity1, Long> {
	
	default Flux<MyEntity1> findByValue(String value) {
		return SelectQuery.from(MyEntity1.class, "entity").where(Criteria.property("entity", "value").is(value)).join("entity", "subEntity", "sub").execute(getLcClient());
	}
	
}
