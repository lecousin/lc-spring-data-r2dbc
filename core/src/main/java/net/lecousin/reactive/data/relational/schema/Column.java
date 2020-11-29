package net.lecousin.reactive.data.relational.schema;

import org.springframework.data.util.Pair;

public class Column {

	private String name;
	private String type;
	private boolean primaryKey;
	private boolean nullable;
	private boolean autoIncrement;
	private Pair<Table, Column> foreignKeyReferences;
	
	public Column(String name) {
		this.name = name;
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

	public String getName() {
		return name;
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

}
