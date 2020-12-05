package net.lecousin.reactive.data.relational.schema.dialect;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.schema.Column;
import net.lecousin.reactive.data.relational.schema.Index;
import net.lecousin.reactive.data.relational.schema.RelationalDatabaseSchema;
import net.lecousin.reactive.data.relational.schema.SchemaException;
import net.lecousin.reactive.data.relational.schema.Table;

@SuppressWarnings({
	"java:S1172", "unused", // unused parameters are present because implementations may need them
	"java:S3400" // we don't want constants
})
public abstract class RelationalDatabaseSchemaDialect {
	
	public Object convertToDataBase(Object value) {
		return value;
	}
	
	public Object convertFromDataBase(Object value, Class<?> targetType) {
		return value;
	}

	@SuppressWarnings("java:S3776") // complexity
	public String getColumnType(Column col, Class<?> type, ColumnDefinition def) {
		if (boolean.class.equals(type) || Boolean.class.equals(type))
			return getColumnTypeBoolean(col, type, def);
		if (byte.class.equals(type) || Byte.class.equals(type))
			return getColumnTypeByte(col, type, def);
		if (short.class.equals(type) || Short.class.equals(type))
			return getColumnTypeShort(col, type, def);
		if (int.class.equals(type) || Integer.class.equals(type))
			return getColumnTypeInteger(col, type, def);
		if (long.class.equals(type) || Long.class.equals(type))
			return getColumnTypeLong(col, type, def);
		if (float.class.equals(type) || Float.class.equals(type))
			return getColumnTypeFloat(col, type, def);
		if (double.class.equals(type) || Double.class.equals(type))
			return getColumnTypeDouble(col, type, def);
		if (BigDecimal.class.equals(type))
			return getColumnTypeBigDecimal(col, type, def);
		if (String.class.equals(type) || char[].class.equals(type))
			return getColumnTypeString(col, type, def);
		if (char.class.equals(type) || Character.class.equals(type))
			return getColumnTypeChar(col, type, def);
		if (java.time.LocalDate.class.equals(type))
			return getColumnTypeDate(col, type, def);
		if (java.time.LocalTime.class.equals(type))
			return getColumnTypeTime(col, type, def);
		if (java.time.OffsetTime.class.equals(type))
			return getColumnTypeTimeWithTimeZone(col, type, def);
		if (java.time.LocalDateTime.class.equals(type))
			return getColumnTypeDateTime(col, type, def);
		if (java.time.ZonedDateTime.class.equals(type))
			return getColumnTypeDateTimeWithTimeZone(col, type, def);
		if (java.time.Instant.class.equals(type))
			return getColumnTypeTimestamp(col, type, def);
		throw new SchemaException("Column type not supported: " + type.getName() + " for column " + col.getName());
	}

	protected String getColumnTypeBoolean(Column col, Class<?> type, ColumnDefinition def) {
		return "BOOLEAN";
	}
	
	protected String getColumnTypeByte(Column col, Class<?> type, ColumnDefinition def) {
		return "TINYINT";
	}
	
	protected String getColumnTypeShort(Column col, Class<?> type, ColumnDefinition def) {
		return "SMALLINT";
	}
	
	protected String getColumnTypeInteger(Column col, Class<?> type, ColumnDefinition def) {
		return "INT";
	}
	
	protected String getColumnTypeLong(Column col, Class<?> type, ColumnDefinition def) {
		return "BIGINT";
	}
	
	protected String getColumnTypeFloat(Column col, Class<?> type, ColumnDefinition def) {
		return "FLOAT";
	}
	
	protected String getColumnTypeDouble(Column col, Class<?> type, ColumnDefinition def) {
		return "DOUBLE";
	}
	
	protected String getColumnTypeBigDecimal(Column col, Class<?> type, ColumnDefinition def) {
		int precision = def != null ? def.precision() : ColumnDefinition.DEFAULT_PRECISION;
		int scale = def != null ? def.scale() : ColumnDefinition.DEFAULT_SCALE;
		return "DECIMAL(" + precision + "," + scale + ")";
	}
	
	protected String getColumnTypeChar(Column col, Class<?> type, ColumnDefinition def) {
		return getColumnTypeShort(col, type, def);
	}
	
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

	protected String getColumnTypeTimestamp(Column col, Class<?> type, ColumnDefinition def) {
		return "TIMESTAMP";
	}

	protected String getColumnTypeDate(Column col, Class<?> type, ColumnDefinition def) {
		return "DATE";
	}

	protected String getColumnTypeTime(Column col, Class<?> type, ColumnDefinition def) {
		return "TIME";
	}

	protected String getColumnTypeTimeWithTimeZone(Column col, Class<?> type, ColumnDefinition def) {
		return "TIME WITH TIME ZONE";
	}

	protected String getColumnTypeDateTime(Column col, Class<?> type, ColumnDefinition def) {
		return "DATETIME";
	}

	protected String getColumnTypeDateTimeWithTimeZone(Column col, Class<?> type, ColumnDefinition def) {
		return "DATETIME WITH TIME ZONE";
	}
	
	public SchemaStatements dropSchemaContent(RelationalDatabaseSchema schema) {
		SchemaStatements toExecute = new SchemaStatements();
		// drop tables
		Map<Table, SchemaStatement> dropTableMap = new HashMap<>();
		for (Table table : schema.getTables()) {
			SchemaStatement dropTable = new SchemaStatement(dropTable(table));
			toExecute.add(dropTable);
			dropTableMap.put(table, dropTable);
		}
		// add dependencies for foreign keys
		for (Table table : schema.getTables()) {
			for (Column col : table.getColumns()) {
				if (col.getForeignKeyReferences() == null)
					continue;
				if (col.getForeignKeyReferences().getFirst() != table)
					dropTableMap.get(col.getForeignKeyReferences().getFirst()).addDependency(dropTableMap.get(table));
			}
		}
		return toExecute;
	}
	
	public String dropTable(Table table) {
		StringBuilder sql = new StringBuilder();
		sql.append("DROP TABLE IF EXISTS ");
		sql.append(table.getName());
		return sql.toString();
	}
	
	public SchemaStatements createSchemaContent(RelationalDatabaseSchema schema) {
		SchemaStatements toExecute = new SchemaStatements();
		// create tables
		Map<Table, SchemaStatement> createTableMap = new HashMap<>();
		for (Table table : schema.getTables()) {
			SchemaStatement createTable = new SchemaStatement(createTable(table));
			createTableMap.put(table, createTable);
			toExecute.add(createTable);
			for (Index index : table.getIndexes()) {
				if (canCreateIndexInTableDefinition(index))
					continue;
				SchemaStatement createIndex = new SchemaStatement(createIndex(table, index));
				createIndex.addDependency(createTable);
				toExecute.add(createIndex);
			}
		}
		// add foreign keys
		Map<Table, List<SchemaStatement>> alterTableByTable = new HashMap<>();
		Map<SchemaStatement, Table> foreignTable = new HashMap<>();
		SchemaStatement latestAlterTable = null;
		for (Table table : schema.getTables()) {
			LinkedList<SchemaStatement> alterTableList = new LinkedList<>();
			StringBuilder sql = new StringBuilder();
			Set<Table> foreignTables = new HashSet<>();
			for (Column col : table.getColumns()) {
				if (col.getForeignKeyReferences() == null)
					continue;
				if (canAddMultipleConstraintsInSingleAlterTable()) {
					if (sql.length() > 0)
						appendForeignKey(table, col, sql);
					else
						sql.append(alterTableForeignKey(table, col));
					foreignTables.add(col.getForeignKeyReferences().getFirst());
				} else {
					SchemaStatement alterTable = new SchemaStatement(alterTableForeignKey(table, col));
					alterTable.addDependency(createTableMap.get(table));
					Table foreign = col.getForeignKeyReferences().getFirst();
					if (foreign != table)
						alterTable.addDependency(createTableMap.get(foreign));
					if (canDoConcurrentAlterTable()) {
						if (!alterTableList.isEmpty())
							alterTable.addDependency(alterTableList.getLast());
						alterTableList.addLast(alterTable);
						if (foreign != table)
							foreignTable.put(alterTable, table);
					} else {
						if (latestAlterTable != null)
							alterTable.addDependency(latestAlterTable);
						latestAlterTable = alterTable;
					}
					toExecute.add(alterTable);
				}
			}
			if (canAddMultipleConstraintsInSingleAlterTable() && sql.length() > 0) {
				SchemaStatement alterTable = new SchemaStatement(sql.toString());
				alterTable.addDependency(createTableMap.get(table));
				for (Table foreign : foreignTables)
					alterTable.addDependency(createTableMap.get(foreign));
				toExecute.add(alterTable);
			}
			alterTableByTable.put(table, alterTableList);
		}
		for (Map.Entry<SchemaStatement, Table> entry : foreignTable.entrySet()) {
			for (SchemaStatement statement : alterTableByTable.get(entry.getValue())) {
				entry.getKey().doNotExecuteTogether(statement);
			}
		}
		return toExecute;
	}
	
	protected boolean canDoConcurrentAlterTable() {
		return true;
	}
	
	protected boolean canAddMultipleConstraintsInSingleAlterTable() {
		return false;
	}
	
	protected boolean canCreateIndexInTableDefinition(Index index) {
		return false;
	}
	
	public String createTable(Table table) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE ").append(table.getName());
		sql.append(" (");
		boolean first = true;
		for (Column col : table.getColumns()) {
			if (first)
				first = false;
			else
				sql.append(", ");
			addColumnDefinition(col, sql);
		}
		for (Index index : table.getIndexes()) {
			if (!canCreateIndexInTableDefinition(index))
				continue;
			sql.append(", ");
			addIndexDefinitionInTable(table, index, sql);
		}
		sql.append(')');
		return sql.toString();
	}
	
	public String createIndex(Table table, Index index) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE ");
		if (index.isUnique())
			sql.append("UNIQUE ");
		sql.append("INDEX ");
		sql.append(index.getName());
		sql.append(" ON ");
		sql.append(table.getName());
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
		return sql.toString();
	}
	
	protected void addColumnDefinition(Column col, StringBuilder sql) {
		sql.append(col.getName());
		sql.append(' ');
		sql.append(col.getType());
		if (!col.isNullable())
			addNotNull(col, sql);
		if (col.isAutoIncrement())
			addAutoIncrement(col, sql);
		if (col.isPrimaryKey())
			addPrimaryKey(col, sql);
	}
	
	protected void addIndexDefinitionInTable(Table table, Index index, StringBuilder sql) {
		// to be overriden if supported
	}
	
	protected void addNotNull(Column col, StringBuilder sql) {
		sql.append(" NOT NULL");
	}
	
	protected void addAutoIncrement(Column col, StringBuilder sql) {
		sql.append(" AUTO_INCREMENT");
	}
	
	protected void addPrimaryKey(Column col, StringBuilder sql) {
		sql.append(" PRIMARY KEY");
	}
	
	protected String alterTableForeignKey(Table table, Column col) {
		StringBuilder sql = new StringBuilder();
		sql.append("ALTER TABLE ");
		sql.append(table.getName());
		addForeignKeyStatement(table, col, sql);
		return sql.toString();
	}
	
	protected void addForeignKeyStatement(Table table, Column col, StringBuilder sql) {
		sql.append(" ADD FOREIGN KEY (");
		sql.append(col.getName());
		sql.append(") REFERENCES ");
		sql.append(col.getForeignKeyReferences().getFirst().getName());
		sql.append('(');
		sql.append(col.getForeignKeyReferences().getSecond().getName());
		sql.append(')');
	}
	
	protected void appendForeignKey(Table table, Column col, StringBuilder sql) {
		sql.append(',');
		addForeignKeyStatement(table, col, sql);
	}
}
