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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Functions;
import org.springframework.data.relational.core.sql.SimpleFunction;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;
import net.lecousin.reactive.data.relational.schema.Column;
import net.lecousin.reactive.data.relational.schema.Index;
import net.lecousin.reactive.data.relational.schema.RelationalDatabaseSchema;
import net.lecousin.reactive.data.relational.schema.SchemaException;
import net.lecousin.reactive.data.relational.schema.Sequence;
import net.lecousin.reactive.data.relational.schema.Table;

/**
 * Base class to implement a database dialect.
 * 
 * @author Guillaume Le Cousin
 *
 */
@SuppressWarnings({
	"java:S1172", "unused", // unused parameters are present because implementations may need them
	"java:S3400" // we don't want constants
})
public abstract class RelationalDatabaseSchemaDialect {
	
	public static final int DEFAULT_FLOATING_POINT_PRECISION = 10;
	public static final int DEFAULT_FLOATING_POINT_SCALE = 2;
	public static final int DEFAULT_TIME_PRECISION = 3;
	
	public interface ColumnTypeMapper {
		String getColumnType(Column col, Type genericType, Class<?> rawType, ColumnDefinition def);
	}
	
	protected Map<Class<?>, ColumnTypeMapper> classToColumnType = new HashMap<>();
	
	protected RelationalDatabaseSchemaDialect() {
		classToColumnType.put(boolean.class, this::getColumnTypeBoolean);
		classToColumnType.put(Boolean.class, this::getColumnTypeBoolean);
		classToColumnType.put(byte.class, this::getColumnTypeByte);
		classToColumnType.put(Byte.class, this::getColumnTypeByte);
		classToColumnType.put(short.class, this::getColumnTypeShort);
		classToColumnType.put(Short.class, this::getColumnTypeShort);
		classToColumnType.put(int.class, this::getColumnTypeInteger);
		classToColumnType.put(Integer.class, this::getColumnTypeInteger);
		classToColumnType.put(long.class, this::getColumnTypeLong);
		classToColumnType.put(Long.class, this::getColumnTypeLong);
		classToColumnType.put(float.class, this::getColumnTypeFloat);
		classToColumnType.put(Float.class, this::getColumnTypeFloat);
		classToColumnType.put(double.class, this::getColumnTypeDouble);
		classToColumnType.put(Double.class, this::getColumnTypeDouble);
		classToColumnType.put(BigDecimal.class, this::getColumnTypeBigDecimal);
		classToColumnType.put(String.class, this::getColumnTypeString);
		classToColumnType.put(char[].class, this::getColumnTypeString);
		classToColumnType.put(char.class, this::getColumnTypeChar);
		classToColumnType.put(Character.class, this::getColumnTypeChar);
		classToColumnType.put(java.time.LocalDate.class, this::getColumnTypeDate);
		classToColumnType.put(java.time.LocalTime.class, this::getColumnTypeTime);
		if (!isTimeZoneSupported())
			classToColumnType.put(java.time.OffsetTime.class, (col, gt, t, def) -> { throw new SchemaException("Time with timezone not supported by " + getName() + " for column " + col.getName() + " with type " + t.getName()); });
		else
			classToColumnType.put(java.time.OffsetTime.class, this::getColumnTypeTimeWithTimeZone);
		classToColumnType.put(java.time.LocalDateTime.class, this::getColumnTypeDateTime);
		if (!isTimeZoneSupported())
			classToColumnType.put(java.time.ZonedDateTime.class, (col, gt, t, def) -> { throw new SchemaException("DateTime with timezone not supported by " + getName() + " for column " + col.getName() + " with type " + t.getName()); });
		else
			classToColumnType.put(java.time.ZonedDateTime.class, this::getColumnTypeDateTimeWithTimeZone);
		classToColumnType.put(java.time.Instant.class, this::getColumnTypeTimestamp);
		classToColumnType.put(UUID.class, this::getColumnTypeUUID);
	}
	
	public abstract String getName();
	
	public abstract boolean isCompatible(R2dbcDialect r2dbcDialect);
	
	public static RelationalDatabaseSchemaDialect getDialect(R2dbcDialect r2dbcDialect) {
		return ServiceLoader.load(RelationalDatabaseSchemaDialect.class).stream()
			.map(Provider::get)
			.filter(dialect -> dialect.isCompatible(r2dbcDialect))
			.findFirst()
			.orElseThrow();
	}
	
	public Object convertToDataBase(Object value, PropertyMetadata property) {
		return value;
	}
	
	public Object convertFromDataBase(Object value, Class<?> targetType) {
		return value;
	}

	public String getColumnType(Column col, Type genericType, ColumnDefinition def) {
		Class<?> type;
		if (genericType instanceof Class) {
			type = (Class<?>)genericType;
		} else if (genericType instanceof ParameterizedType) {
			type = (Class<?>)((ParameterizedType)genericType).getRawType();
		} else {
			throw new SchemaException("Column type not supported: " + genericType + " on column " + col.getName() + " with " + getName());
		}
		ColumnTypeMapper mapper = classToColumnType.get(type);
		if (mapper != null)
			return mapper.getColumnType(col, genericType, type, def);
		if (Enum.class.isAssignableFrom(type))
			return getColumnTypeEnum(col, genericType, type, def);
		if (isArrayColumnSupported()) {
			Type elementType = null;
			if (type.isArray())
				elementType = type.getComponentType();
			else if (Collection.class.isAssignableFrom(type) && genericType instanceof ParameterizedType)
				elementType = ((ParameterizedType)genericType).getActualTypeArguments()[0];
			if (elementType != null) {
				return getArrayColumnType(col, genericType, type, elementType, def);
			}
		}
		return customColumnTypes(col, genericType, type, def);
	}
	
	protected String customColumnTypes(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		throw new SchemaException("Column type not supported: " + type.getName() + " on column " + col.getName() + " with " + getName());
	}

	protected String getColumnTypeBoolean(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		return "BOOLEAN";
	}
	
	protected String getColumnTypeByte(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		return "TINYINT";
	}
	
	protected String getColumnTypeShort(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		return "SMALLINT";
	}
	
	protected String getColumnTypeInteger(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		return "INT";
	}
	
	protected String getColumnTypeLong(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		return "BIGINT";
	}
	
	protected String getColumnTypeFloat(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		return "FLOAT";
	}
	
	protected String getColumnTypeDouble(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		return "DOUBLE";
	}
	
	protected String getColumnTypeBigDecimal(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		int precision = def != null ? def.precision() : -1;
		if (precision < 0)
			precision = DEFAULT_FLOATING_POINT_PRECISION;
		int scale = def != null ? def.scale() : -1;
		if (scale < 0)
			scale = DEFAULT_FLOATING_POINT_SCALE;
		return "DECIMAL(" + precision + "," + scale + ")";
	}
	
	protected String getColumnTypeChar(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		return getColumnTypeShort(col, genericType, type, def);
	}
	
	protected String getColumnTypeString(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		if (def != null) {
			if (def.max() > Integer.MAX_VALUE) {
				// large text
				return "CLOB(" + def.max() + ")";
			}
			if (def.max() > 0) {
				// max length
				return "VARCHAR(" + def.max() + ")";
			}
		}
		return "VARCHAR";
	}
	
	public boolean isTimeZoneSupported() {
		return true;
	}

	protected String getColumnTypeTimestamp(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		int precision = def != null ? def.precision() : -1;
		if (precision < 0)
			precision = DEFAULT_TIME_PRECISION;
		return "TIMESTAMP(" + precision + ")";
	}

	protected String getColumnTypeDate(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		return "DATE";
	}

	protected String getColumnTypeTime(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		int precision = def != null ? def.precision() : -1;
		if (precision < 0)
			precision = DEFAULT_TIME_PRECISION;
		return "TIME(" + precision + ")";
	}

	protected String getColumnTypeTimeWithTimeZone(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		int precision = def != null ? def.precision() : -1;
		if (precision < 0)
			precision = DEFAULT_TIME_PRECISION;
		return "TIME(" + precision + ") WITH TIME ZONE";
	}

	protected String getColumnTypeDateTime(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		int precision = def != null ? def.precision() : -1;
		if (precision < 0)
			precision = DEFAULT_TIME_PRECISION;
		return "DATETIME(" + precision + ")";
	}

	protected String getColumnTypeDateTimeWithTimeZone(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		int precision = def != null ? def.precision() : -1;
		if (precision < 0)
			precision = DEFAULT_TIME_PRECISION;
		return "DATETIME(" + precision + ") WITH TIME ZONE";
	}

	protected String getColumnTypeUUID(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		return "UUID";
	}

	protected String getColumnTypeEnum(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		int max = 1;
		for (Object enumValue : type.getEnumConstants()) {
			max = Math.max(max, enumValue.toString().length());
		}
		return "VARCHAR(" + max + ")";
	}
	
	public boolean isArrayColumnSupported() {
		return false;
	}
	
	protected String getArrayColumnType(Column col, Type genericType, Class<?> type, Type genericElementType, ColumnDefinition def) {
		throw new SchemaException("Array column not supported");
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
		// drop sequences
		if (supportsSequence())
			for (Sequence s : schema.getSequences())
				toExecute.add(new SchemaStatement(dropSequence(s)));
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
		Map<Table, SchemaStatement> createTableMap = createTables(schema, toExecute);
		addConstraints(schema, toExecute, createTableMap);
		createSequences(schema, toExecute);
		return toExecute;
	}
	
	private Map<Table, SchemaStatement> createTables(RelationalDatabaseSchema schema, SchemaStatements toExecute) {
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
		return createTableMap;
	}
	
	private void createSequences(RelationalDatabaseSchema schema, SchemaStatements toExecute) {
		if (!supportsSequence())
			return;
		for (Sequence s : schema.getSequences())
			toExecute.add(new SchemaStatement(createSequence(s)));
	}
	
	private void addConstraints(RelationalDatabaseSchema schema, SchemaStatements toExecute, Map<Table, SchemaStatement> createTableMap) {
		Map<Table, List<SchemaStatement>> alterTableByTable = new HashMap<>();
		Map<SchemaStatement, Table> foreignTable = new HashMap<>();
		MutableObject<SchemaStatement> latestAlterTable = new MutableObject<>(null);
		for (Table table : schema.getTables()) {
			addTableConstraints(table, toExecute, createTableMap, alterTableByTable, foreignTable, latestAlterTable);
		}
		for (Map.Entry<SchemaStatement, Table> entry : foreignTable.entrySet()) {
			for (SchemaStatement statement : alterTableByTable.get(entry.getValue())) {
				entry.getKey().doNotExecuteTogether(statement);
			}
		}
	}
	
	private void addTableConstraints(Table table, SchemaStatements toExecute, Map<Table, SchemaStatement> createTableMap, Map<Table, List<SchemaStatement>> alterTableByTable, Map<SchemaStatement, Table> foreignTable, MutableObject<SchemaStatement> latestAlterTable) {
		LinkedList<SchemaStatement> alterTableList = new LinkedList<>();
		StringBuilder sql = new StringBuilder();
		Set<Table> foreignTables = new HashSet<>();
		foreignTables.add(table);
		for (Column col : table.getColumns()) {
			if (col.getForeignKeyReferences() == null)
				continue;
			if (canAddMultipleConstraintsInSingleAlterTable()) {
				appendForeignKeyConstraint(table, col, sql);
				foreignTables.add(col.getForeignKeyReferences().getFirst());
			} else {
				toExecute.add(createAlterTableAddForeignKey(table, col, createTableMap, alterTableList, foreignTable, latestAlterTable));
			}
		}
		if (canAddMultipleConstraintsInSingleAlterTable() && sql.length() > 0) {
			SchemaStatement alterTable = new SchemaStatement(sql.toString());
			for (Table foreign : foreignTables)
				alterTable.addDependency(createTableMap.get(foreign));
			if (!canDoConcurrentAlterTable())
				addAlterTable(latestAlterTable, alterTable);
			toExecute.add(alterTable);
		}
		alterTableByTable.put(table, alterTableList);
	}
	
	private void appendForeignKeyConstraint(Table table, Column col, StringBuilder sql) {
		if (sql.length() > 0)
			appendForeignKey(table, col, sql);
		else
			sql.append(alterTableForeignKey(table, col));
	}
	
	private SchemaStatement createAlterTableAddForeignKey(Table table, Column col, Map<Table, SchemaStatement> createTableMap, LinkedList<SchemaStatement> alterTableList, Map<SchemaStatement, Table> foreignTable, MutableObject<SchemaStatement> latestAlterTable) {
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
			addAlterTable(latestAlterTable, alterTable);
		}
		return alterTable;
	}
	
	private static void addAlterTable(MutableObject<SchemaStatement> latestAlterTable, SchemaStatement alterTable) {
		if (latestAlterTable.getValue() != null)
			alterTable.addDependency(latestAlterTable.getValue());
		latestAlterTable.setValue(alterTable);
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
		if (col.isRandomUuid() && supportsUuidGeneration())
			addDefaultRandomUuid(col, sql);
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
	
	public boolean supportsUuidGeneration() {
		return true;
	}
	
	protected void addDefaultRandomUuid(Column col, StringBuilder sql) {
		sql.append(" DEFAULT RANDOM_UUID()");
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
	
	public boolean supportsSequence() {
		return true;
	}

	protected String dropSequence(Sequence sequence) {
		return "DROP SEQUENCE IF EXISTS " + sequence.getName();
	}
	
	protected String createSequence(Sequence sequence) {
		return "CREATE SEQUENCE " + sequence.getName() + " START WITH 1 INCREMENT BY 1";
	}
	
	public String sequenceNextValueFunctionName() {
		return "NEXTVAL";
	}
	
	public enum SqlFunction {
		UPPER, LOWER,
		ISO_DAY_OF_WEEK, DAY_OF_MONTH, DAY_OF_YEAR, MONTH, YEAR, ISO_WEEK, HOUR, MINUTE, SECOND
	}
	
	public Expression applyFunctionTo(SqlFunction function, Expression expression) {
		switch (function) {
		case DAY_OF_MONTH: return SimpleFunction.create("DAY_OF_MONTH", Collections.singletonList(expression));
		case DAY_OF_YEAR: return SimpleFunction.create("DAY_OF_YEAR", Collections.singletonList(expression));
		case HOUR: return SimpleFunction.create("HOUR", Collections.singletonList(expression));
		case ISO_DAY_OF_WEEK: return SimpleFunction.create("ISO_DAY_OF_WEEK", Collections.singletonList(expression));
		case ISO_WEEK: return SimpleFunction.create("ISO_WEEK", Collections.singletonList(expression));
		case LOWER: return SimpleFunction.create("LOWER", Collections.singletonList(expression));
		case MINUTE: return SimpleFunction.create("MINUTE", Collections.singletonList(expression));
		case MONTH: return SimpleFunction.create("MONTH", Collections.singletonList(expression));
		case SECOND: return SimpleFunction.create("SECOND", Collections.singletonList(expression));
		case UPPER: return SimpleFunction.create("UPPER", Collections.singletonList(expression));
		case YEAR: return SimpleFunction.create("YEAR", Collections.singletonList(expression));
		}
		throw new SchemaException("Unknown SQL function: " + function);
	}
	
	public Expression countDistinct(List<Expression> expressions) {
		return Functions.count(SimpleFunction.create("DISTINCT", expressions));
	}
	
	public boolean isMultipleInsertSupported() {
		return true;
	}
}
