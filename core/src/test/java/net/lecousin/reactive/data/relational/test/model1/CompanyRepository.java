package net.lecousin.reactive.data.relational.test.model1;

import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Mono;

public interface CompanyRepository extends LcR2dbcRepository<Company, Long> {

	Mono<Company> findByName(String name);
	
}
