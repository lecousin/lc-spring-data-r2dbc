package net.lecousin.reactive.data.relational.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.NoRepositoryBean;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;

@NoRepositoryBean
public interface LcR2dbcRepository<T, ID> extends R2dbcRepository<T, ID> {

	LcReactiveDataRelationalClient getLcClient();
	
}
