package net.lecousin.reactive.data.relational.test.onetoonemodel;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Flux;

public interface MyEntity4Repository extends LcR2dbcRepository<MyEntity4, Long> {

	default Flux<MyEntity4> findAllWithSubEntities() {
		return SelectQuery.from(MyEntity4.class, "e").join("e", "subEntity", "s").join("s", "entity1", "e1").execute(getLcClient());
	}
	
}
