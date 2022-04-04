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

/**
 * Represent an index on columns in a database schema.
 * 
 * @author Guillaume Le Cousin
 *
 */
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
