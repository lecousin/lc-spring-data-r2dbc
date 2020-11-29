package net.lecousin.reactive.data.relational.mysql;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.schema.Column;
import net.lecousin.reactive.data.relational.schema.Index;
import net.lecousin.reactive.data.relational.schema.Table;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;

public class MySqlSchemaDialect extends RelationalDatabaseSchemaDialect {

	@Override
	public Object convertToDataBase(Object value) {
		if (value instanceof java.time.OffsetTime || value instanceof java.time.ZonedDateTime)
			return value.toString();
		return super.convertToDataBase(value);
	}
	
	@Override
	public Object convertFromDataBase(Object value, Class<?> targetType) {
		if (java.time.OffsetTime.class.equals(targetType))
			return java.time.OffsetTime.parse((CharSequence)value);
		if (java.time.ZonedDateTime.class.equals(targetType))
			return java.time.ZonedDateTime.parse((CharSequence)value);
		return super.convertFromDataBase(value, targetType);
	}
	
	@Override
	protected String getColumnTypeString(Column col, Class<?> type, ColumnDefinition def) {
		if (def != null) {
			if (def.min() > 0 && def.max() == def.min() && def.max() <= 255) {
				// fixed length <= 255
				return "CHAR(" + def.max() + ")";
			}
			if (def.max() > 255) {
				// large text
				return "LONGTEXT";
			}
			if (def.max() > 0) {
				// max length
				return "VARCHAR(" + def.max() + ")";
			}
		}
		return "VARCHAR(255)";
	}

	@Override
	protected String getColumnTypeTimeWithTimeZone(Column col, Class<?> type, ColumnDefinition def) {
		return "VARCHAR(24)";
	}
	
	@Override
	protected String getColumnTypeDateTimeWithTimeZone(Column col, Class<?> type, ColumnDefinition def) {
		return "VARCHAR(100)";
	}
	
	@Override
	protected boolean canCreateIndexInTableDefinition(Index index) {
		return true;
	}
	
	@Override
	protected void addIndexDefinitionInTable(Table table, Index index, StringBuilder sql) {
		if (index.isUnique())
			sql.append("CONSTRAINT UNIQUE INDEX ");
		else
			sql.append("INDEX ");
		sql.append(index.getName());
		sql.append('(');
		boolean first = true;
		for (String col : index.getColumns()) {
			if (first)
				first = false;
			else
				sql.append(',');
			sql.append(col);
		}
		sql.append(')');
	}
}
