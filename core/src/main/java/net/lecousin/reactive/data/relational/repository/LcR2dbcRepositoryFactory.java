package net.lecousin.reactive.data.relational.repository;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.data.repository.core.RepositoryMetadata;

public class LcR2dbcRepositoryFactory extends R2dbcRepositoryFactory {

	public LcR2dbcRepositoryFactory(R2dbcEntityOperations operations) {
		super(operations);
	}
	
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return LcR2dbcRepositoryImpl.class;
	}
	
}
