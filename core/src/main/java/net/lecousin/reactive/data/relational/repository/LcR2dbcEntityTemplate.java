package net.lecousin.reactive.data.relational.repository;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;

public class LcR2dbcEntityTemplate extends R2dbcEntityTemplate {

	private LcReactiveDataRelationalClient lcClient;
	
	public LcR2dbcEntityTemplate(LcReactiveDataRelationalClient lcClient) {
		super(lcClient.getSpringClient(), lcClient.getDataAccess());
		this.lcClient = lcClient;
	}

	public LcReactiveDataRelationalClient getLcClient() {
		return lcClient;
	}
	
}
