package net.lecousin.reactive.data.relational.test.model1;

import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Flux;

public interface PersonRepository extends LcR2dbcRepository<Person, Long> {

	Flux<Person> findByFirstName(String firstName);
	
}
