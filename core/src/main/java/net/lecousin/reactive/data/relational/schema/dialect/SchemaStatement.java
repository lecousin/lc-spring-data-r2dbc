package net.lecousin.reactive.data.relational.schema.dialect;

import java.util.HashSet;
import java.util.Set;

public class SchemaStatement {

	private String sql;
	private Set<SchemaStatement> dependencies = new HashSet<>();

	public SchemaStatement(String sql) {
		this.sql = sql;
	}
	
	public void addDependency(SchemaStatement dependsOn) {
		dependencies.add(dependsOn);
	}
	
	public void removeDependency(SchemaStatement statement) {
		dependencies.remove(statement);
	}
	
	public boolean hasDependency() {
		return !dependencies.isEmpty();
	}
	
	public String getSql() {
		return sql;
	}
	
}
