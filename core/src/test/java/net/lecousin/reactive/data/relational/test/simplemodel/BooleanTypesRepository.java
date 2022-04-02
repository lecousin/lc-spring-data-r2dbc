package net.lecousin.reactive.data.relational.test.simplemodel;

import org.springframework.data.r2dbc.repository.Query;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Flux;

public interface BooleanTypesRepository extends LcR2dbcRepository<BooleanTypes, Long> {

	default Flux<BooleanTypes> page(int start, int nb) {
		return SelectQuery.from(BooleanTypes.class, "b").limit(start, nb).execute(getLcClient());
	}
	
	@Query("select b1, b2 from boolean_types")
	Flux<BooleanTypes> findAllWithoutId();
	
}
