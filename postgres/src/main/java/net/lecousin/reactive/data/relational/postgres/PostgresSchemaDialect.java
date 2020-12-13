package net.lecousin.reactive.data.relational.postgres;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.schema.Column;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;

public class PostgresSchemaDialect extends RelationalDatabaseSchemaDialect {

	@Override
	protected void addAutoIncrement(Column col, StringBuilder sql) {
		// nothing to add
	}
	
	@Override
	protected String getColumnTypeByte(Column col, Class<?> type, ColumnDefinition def) {
		return getColumnTypeShort(col, type, def);
	}
	
	@Override
	protected String getColumnTypeShort(Column col, Class<?> type, ColumnDefinition def) {
		if (col.isAutoIncrement())
			return "SMALLSERIAL";
		return "SMALLINT";
	}
	
	@Override
	protected String getColumnTypeInteger(Column col, Class<?> type, ColumnDefinition def) {
		if (col.isAutoIncrement())
			return "SERIAL";
		return "INTEGER";
	}
	
	@Override
	protected String getColumnTypeLong(Column col, Class<?> type, ColumnDefinition def) {
		if (col.isAutoIncrement())
			return "BIGSERIAL";
		return "BIGINT";
	}
	
	@Override
	protected String getColumnTypeFloat(Column col, Class<?> type, ColumnDefinition def) {
		return "REAL";
	}
	
	@Override
	protected String getColumnTypeDouble(Column col, Class<?> type, ColumnDefinition def) {
		return "DOUBLE PRECISION";
	}
	
	@Override
	protected String getColumnTypeDateTime(Column col, Class<?> type, ColumnDefinition def) {
		return "TIMESTAMP";
	}
	
	@Override
	protected String getColumnTypeDateTimeWithTimeZone(Column col, Class<?> type, ColumnDefinition def) {
		return "TIMESTAMP WITH TIME ZONE";
	}

	@Override
	protected String getColumnTypeString(Column col, Class<?> type, ColumnDefinition def) {
		if (def != null) {
			if (def.max() > Integer.MAX_VALUE) {
				// large text
				return "CLOB(" + def.max() + ")";
			}
			if (def.min() > 0 && def.max() == def.min()) {
				// fixed length
				return "CHAR(" + def.max() + ")";
			}
			if (def.max() > 0) {
				// max length
				return "VARCHAR(" + def.max() + ")";
			}
		}
		return "VARCHAR";
	}

	@Override
	protected void addDefaultRandomUuid(Column col, StringBuilder sql) {
		sql.append(" DEFAULT UUID_GENERATE_V4()");
	}
}
