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
 * A relational Table, containing columns and indexes.
 * 
 * @author Guillaume Le Cousin
 *
 */
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

	public Column getColumn(String name) {
		for (Column col : columns)
			if (col.getName().equals(name))
				return col;
		throw new NoSuchElementException("Column <" + name + "> in table <" + name + ">");
	}
}
