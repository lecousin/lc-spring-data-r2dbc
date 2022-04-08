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

import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.util.Pair;

/**
 * A column in a table.
 * 
 * @author Guillaume Le Cousin
 *
 */
public class Column {

	private Table table;
	private SqlIdentifier sqlId;
	private String type;
	private boolean primaryKey;
	private boolean nullable;
	private boolean autoIncrement;
	private boolean randomUuid;
	private Pair<Table, Column> foreignKeyReferences;
	
	public Column(Table table, SqlIdentifier sqlId) {
		this.table = table;
		this.sqlId = sqlId;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(boolean primaryKey) {
		this.primaryKey = primaryKey;
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	public void setAutoIncrement(boolean autoIncrement) {
		this.autoIncrement = autoIncrement;
	}

	public boolean isRandomUuid() {
		return randomUuid;
	}

	public void setRandomUuid(boolean randomUuid) {
		this.randomUuid = randomUuid;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Pair<Table, Column> getForeignKeyReferences() {
		return foreignKeyReferences;
	}

	public void setForeignKeyReferences(Pair<Table, Column> foreignKeyReferences) {
		this.foreignKeyReferences = foreignKeyReferences;
	}
	
	public String toSql() {
		return sqlId.toSql(table.idProcessing());
	}
	
	public String getReferenceName() {
		return sqlId.getReference();
	}

}
