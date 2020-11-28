package net.lecousin.reactive.data.relational.repository;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.r2dbc.core.DatabaseClient;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;

public class LcR2dbcRepositoryFactory extends R2dbcRepositoryFactory {

	private LcReactiveDataRelationalClient lcClient;
	
	public LcR2dbcRepositoryFactory(DatabaseClient databaseClient, ReactiveDataAccessStrategy dataAccessStrategy, LcReactiveDataRelationalClient lcClient) {
		super(databaseClient, dataAccessStrategy);
		this.lcClient = lcClient;
	}

	public LcR2dbcRepositoryFactory(R2dbcEntityOperations operations, LcReactiveDataRelationalClient lcClient) {
		super(operations);
		this.lcClient = lcClient;
	}
	
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return LcR2dbcRepositoryImpl.class;
	}
	
	@Override
	protected Object getTargetRepository(RepositoryInformation information) {
		LcR2dbcRepositoryImpl<?, ?> repo = (LcR2dbcRepositoryImpl<?, ?>) super.getTargetRepository(information);
		repo.setLcClient(lcClient);
		return repo;
	}

}
