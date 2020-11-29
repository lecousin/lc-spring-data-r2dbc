package net.lecousin.reactive.data.relational.test.simplemodel;

import org.springframework.data.r2dbc.repository.Query;

import io.r2dbc.spi.Row;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Flux;

public interface NumericTypesRepository extends LcR2dbcRepository<NumericTypes, Long> {

	Flux<Row> findByLong1(long long1);
	
	@Query("SELECT id, long1 FROM NUMTYPES")
	Flux<NumericTypes> getAllOnlyWithIdAndLong1();

	@Query("SELECT long1 FROM NUMTYPES")
	Flux<Long> getAllLong1();
	
}
