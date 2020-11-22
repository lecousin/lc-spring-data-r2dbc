package net.lecousin.reactive.data.relational.mapping;

public class ResultMappingContext {

	private ResultEntityCache entityCache;
	
	public ResultMappingContext() {
		this(new ResultEntityCache());
	}
	
	public ResultMappingContext(ResultEntityCache entityCache) {
		this.entityCache = entityCache;
	}
	
	public ResultEntityCache getEntityCache() {
		return entityCache;
	}

}
