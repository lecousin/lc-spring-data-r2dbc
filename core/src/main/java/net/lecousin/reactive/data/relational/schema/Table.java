package net.lecousin.reactive.data.relational.schema;

import java.util.LinkedList;
import java.util.List;

public class Table {

	private String name;
	private List<Column> columns = new LinkedList<>();
	private List<Index> indexes = new LinkedList<>();
	
	public Table(String name) {
		this.name = name;
	}
	
	public void add(Column col) {
		columns.add(col);
	}
	
	public void add(Index index) {
		indexes.add(index);
	}

	public String getName() {
		return name;
	}

	public List<Column> getColumns() {
		return columns;
	}
	
	public List<Index> getIndexes() {
		return indexes;
	}

}
