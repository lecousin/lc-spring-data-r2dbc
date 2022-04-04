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
package net.lecousin.reactive.data.relational.schema.dialect;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A schema statement (create table, add constraints, add index ...).
 * 
 * @author Guillaume Le Cousin
 *
 */
public class SchemaStatement {

	private String sql;
	private Set<SchemaStatement> dependencies = new HashSet<>();
	private Set<SchemaStatement> doNotExecuteTogether = new HashSet<>();

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
	
	public void doNotExecuteTogether(SchemaStatement statement) {
		doNotExecuteTogether.add(statement);
	}
	
	public boolean canExecuteWith(List<SchemaStatement> statements) {
		return doNotExecuteTogether.stream().noneMatch(statements::contains);
	}
	
	public String getSql() {
		return sql;
	}
	
}
