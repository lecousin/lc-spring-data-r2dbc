/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lecousin.reactive.data.relational.schema;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Database schema, with tables and sequences.
 * 
 * @author Guillaume Le Cousin
 *
 */
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
			if (table.getReferenceName().equalsIgnoreCase(name))
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
			if (sequence.getReferenceName().equalsIgnoreCase(name))
				return sequence;
		throw new NoSuchElementException("Sequence " + name);
	}
}
