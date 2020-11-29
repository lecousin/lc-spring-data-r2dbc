package net.lecousin.reactive.data.relational.schema;

import java.util.LinkedList;
import java.util.List;

public class Table {

	private String name;
	private List<Column> columns = new LinkedList<>();
	
	public Table(String name) {
		this.name = name;
	}
	
	public void add(Column col) {
		columns.add(col);
	}

	public String getName() {
		return name;
	}

	public List<Column> getColumns() {
		return columns;
	}

}
