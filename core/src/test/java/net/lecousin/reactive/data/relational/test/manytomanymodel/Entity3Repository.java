package net.lecousin.reactive.data.relational.test.manytomanymodel;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Flux;

public interface Entity3Repository extends LcR2dbcRepository<Entity3, Long> {
	
	default Flux<Entity3> findByEntity3Value(String value) {
		return SelectQuery.from(Entity3.class, "e1").where(Criteria.property("e1", "value").is(value)).join("e1", "links", "e2").execute(getLcClient());
	}
	
	default Flux<Entity3> findByEntity4Value(String value) {
		return SelectQuery.from(Entity3.class, "e1").where(Criteria.property("e2", "value").is(value)).join("e1", "links", "e2").execute(getLcClient());
	}
	
	default Flux<Entity3> findByLinkedEntity3Value(String value) {
		return SelectQuery.from(Entity3.class, "e1").where(Criteria.property("e1bis", "value").is(value)).join("e1", "links", "e2").join("e2", "links", "e1bis").execute(getLcClient());
	}
	
	default Flux<Entity3> findWithLinks() {
		return SelectQuery.from(Entity3.class, "e1").join("e1", "links", "e2").execute(getLcClient());
	}

}
