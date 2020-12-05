package net.lecousin.reactive.data.relational.test.treemodel;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.query.criteria.Criteria;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Flux;

public interface NodeRepository extends LcR2dbcRepository<Node, Long> {

	default Flux<Node> fetch1Level() {
		return SelectQuery.from(Node.class, "root")
			.where(Criteria.property("root", "parent").isNull())
			.join("root", "children", "sub1")
			.execute(getLcClient());
	}

	default Flux<Node> fetch2Levels() {
		return SelectQuery.from(Node.class, "root")
			.where(Criteria.property("root", "parent").isNull())
			.join("root", "children", "sub1")
			.join("sub1", "children", "sub2")
			.execute(getLcClient());
	}
	
}
