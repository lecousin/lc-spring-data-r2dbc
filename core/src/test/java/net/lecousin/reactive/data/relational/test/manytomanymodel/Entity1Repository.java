package net.lecousin.reactive.data.relational.test.manytomanymodel;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Entity1Repository extends LcR2dbcRepository<Entity1, Long> {
	
	default Flux<Entity1> findByEntity1Value(String value) {
		return SelectQuery.from(Entity1.class, "e1").where(Criteria.property("e1", "value").is(value)).join("e1", "links", "link1").join("link1", "entity2", "e2").execute(getLcClient());
	}
	
	default Flux<Entity1> findByEntity2Value(String value) {
		return SelectQuery.from(Entity1.class, "e1").where(Criteria.property("e2", "value").is(value)).join("e1", "links", "link1").join("link1", "entity2", "e2").execute(getLcClient());
	}
	
	default Flux<Entity1> findByLinkedEntity1Value(String value) {
		return SelectQuery.from(Entity1.class, "e1").where(Criteria.property("e1bis", "value").is(value)).join("e1", "links", "link1").join("link1", "entity2", "e2").join("e2", "links", "link2").join("link2", "entity1", "e1bis").execute(getLcClient());
	}
	
	default Mono<Long> countByLinkedEntity1Value(String value) {
		return SelectQuery.from(Entity1.class, "e1").where(Criteria.property("e1bis", "value").is(value)).join("e1", "links", "link1").join("link1", "entity2", "e2").join("e2", "links", "link2").join("link2", "entity1", "e1bis").executeCount(getLcClient());
	}
	
	default Flux<Entity1> findWithLinks() {
		return SelectQuery.from(Entity1.class, "e1").join("e1", "links", "link1").join("link1", "entity2", "e2").execute(getLcClient());
	}

}
