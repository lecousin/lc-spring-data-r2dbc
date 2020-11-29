package net.lecousin.reactive.data.relational.schema;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class RelationalDatabaseSchema {

	private List<Table> tables = new LinkedList<>();
	
	public void add(Table table) {
		tables.add(table);
	}

	public List<Table> getTables() {
		return tables;
	}
	
	public Table getTable(String name) {
		for (Table table : tables)
			if (table.getName().equals(name))
				return table;
		throw new NoSuchElementException("Table " + name);
	}
}
