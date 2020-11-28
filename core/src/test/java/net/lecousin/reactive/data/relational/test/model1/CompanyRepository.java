package net.lecousin.reactive.data.relational.test.model1;

import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.repository.LcR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CompanyRepository extends LcR2dbcRepository<Company, Long> {

	Mono<Company> findByName(String name);
	
	default Flux<Company> findAllWithJoins() {
		return SelectQuery.from(Company.class, "company")
			.join("company", "owner", "companyOwner")
			.join("companyOwner", "address", "companyOwnerAddress")
			.join("company", "employees", "employee")
			.join("employee", "person", "employeePerson")
			.join("employeePerson", "address", "employeeAddress")
			.join("company", "sites", "site")
			.join("site", "address", "siteAddress")
			.join("company", "providers", "provider")
			.join("provider", "person", "providerPerson")
			.join("providerPerson", "address", "providerPersonAddress")
			.join("provider", "provider", "providerCompany")
			.execute(getLcClient());
	}
	
	default Flux<Company> findAllWithJoins(int start, int nb) {
		return SelectQuery.from(Company.class, "company")
			.join("company", "owner", "companyOwner")
			.join("companyOwner", "address", "companyOwnerAddress")
			.join("company", "employees", "employee")
			.join("employee", "person", "employeePerson")
			.join("employeePerson", "address", "employeeAddress")
			.join("company", "sites", "site")
			.join("site", "address", "siteAddress")
			.join("company", "providers", "provider")
			.join("provider", "person", "providerPerson")
			.join("providerPerson", "address", "providerPersonAddress")
			.join("provider", "provider", "providerCompany")
			.limit(start, nb)
			.execute(getLcClient());
	}
	
}
