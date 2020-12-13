package net.lecousin.reactive.data.relational.schema;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class RelationalDatabaseSchema {

	private List<Table> tables = new LinkedList<>();
	private List<Sequence> sequences = new LinkedList<>();
	
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
	
	public void add(Sequence sequence) {
		sequences.add(sequence);
	}
	
	public List<Sequence> getSequences() {
		return sequences;
	}
	
	public Sequence getSequence(String name) {
		for (Sequence sequence : sequences)
			if (sequence.getName().equals(name))
				return sequence;
		throw new NoSuchElementException("Sequence " + name);
	}
}
