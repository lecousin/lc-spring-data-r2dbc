package net.lecousin.reactive.data.relational.schema;

import java.util.LinkedList;
import java.util.List;

public class Index {

	private String name;
	private List<String> columns = new LinkedList<>();
	private boolean unique;
	
	public Index(String name) {
		this.name = name;
	}
	
	public void addColumn(String col) {
		columns.add(col);
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public String getName() {
		return name;
	}

	public List<String> getColumns() {
		return columns;
	}

}
