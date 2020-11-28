package net.lecousin.reactive.data.relational.test.simplemodel;

import io.r2dbc.spi.Row;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Flux;

public interface NumericTypesRepository extends LcR2dbcRepository<NumericTypes, Long> {

	Flux<Row> findByLong1(long long1);
	
}
