package net.lecousin.reactive.data.relational.schema;

import java.util.LinkedList;
import java.util.List;

public class RelationalDatabaseSchema {

	private List<Table> tables = new LinkedList<>();
	
	public void add(Table table) {
		tables.add(table);
	}

	public List<Table> getTables() {
		return tables;
	}
	
}
