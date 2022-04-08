package net.lecousin.reactive.data.relational.mysql;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.r2dbc.dialect.MySqlDialect;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Expressions;
import org.springframework.data.relational.core.sql.Functions;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.SimpleFunction;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;
import net.lecousin.reactive.data.relational.schema.Column;
import net.lecousin.reactive.data.relational.schema.Index;
import net.lecousin.reactive.data.relational.schema.Table;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;

public class MySqlSchemaDialect extends RelationalDatabaseSchemaDialect {

	@Override
	public String getName() {
		return "MySQL";
	}
	
	@Override
	public boolean isCompatible(R2dbcDialect r2dbcDialect) {
		return r2dbcDialect.getClass().equals(MySqlDialect.class);
	}
	
	@Override
	public Object convertToDataBase(Object value, PropertyMetadata property) {
		if (value instanceof String) {
			ColumnDefinition def = property.getRequiredSpringProperty().findAnnotation(ColumnDefinition.class);
			if (def != null && def.min() > 0 && ((String)value).length() < def.min())
				value = StringUtils.rightPad((String)value, (int)def.min(), ' ');
		}
		if (value instanceof UUID)
			value = ((UUID)value).toString();
		return super.convertToDataBase(value, property);
	}
	
	@Override
	public Object convertFromDataBase(Object value, Class<?> targetType) {
		if (UUID.class.equals(targetType))
			return UUID.fromString((String)value);
		if (value instanceof Long) {
			if (Byte.class.equals(targetType) || byte.class.equals(targetType))
				value = ((Long)value).byteValue();
			else if (Short.class.equals(targetType) || short.class.equals(targetType))
				value = ((Long)value).shortValue();
			else if (Integer.class.equals(targetType) || int.class.equals(targetType))
				value = ((Long)value).intValue();
		}
		return super.convertFromDataBase(value, targetType);
	}
	
	@Override
	protected String getColumnTypeString(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		if (def != null) {
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
	public boolean isTimeZoneSupported() {
		return false;
	}

	@Override
	protected String getColumnTypeUUID(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		return "VARCHAR(36)";
	}
	
	@Override
	protected String getColumnTypeEnum(Column col, Type genericType, Class<?> type, ColumnDefinition def) {
		StringBuilder s = new StringBuilder(256);
		s.append("ENUM(");
		boolean first = true;
		for (Object enumValue : type.getEnumConstants()) {
			if (first)
				first = false;
			else
				s.append(',');
			s.append('\'').append(enumValue.toString()).append('\'');
		}
		s.append(')');
		return s.toString();
	}
	
	@Override
	public boolean supportsUuidGeneration() {
		return false;
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
		sql.append(index.toSql());
		sql.append('(');
		boolean first = true;
		for (Column col : index.getColumns()) {
			if (first)
				first = false;
			else
				sql.append(',');
			sql.append(col.toSql());
		}
		sql.append(')');
	}
	
	@Override
	protected boolean canDoConcurrentAlterTable() {
		return false;
	}
	
	@Override
	protected boolean canAddMultipleConstraintsInSingleAlterTable() {
		return true;
	}
	
	@Override
	public boolean supportsSequence() {
		return false;
	}
	
	@Override
	public Expression applyFunctionTo(SqlFunction function, Expression expression) {
		switch (function) {
		case DAY_OF_MONTH: return SimpleFunction.create("DAYOFMONTH", Collections.singletonList(expression));
		case DAY_OF_YEAR: return SimpleFunction.create("DAYOFYEAR", Collections.singletonList(expression));
		case ISO_WEEK: return SimpleFunction.create("WEEK", Arrays.asList(expression, SQL.literalOf(3)));
		case ISO_DAY_OF_WEEK: 
			return Expressions.just("(" + SimpleFunction.create("WEEKDAY", Collections.singletonList(expression)) + " + 1)");
		default: break;
		}
		return super.applyFunctionTo(function, expression);
	}
	
	@Override
	public Expression countDistinct(List<Expression> expressions) {
		if (expressions.size() == 1)
			return super.countDistinct(expressions);
		List<Expression> concat = new ArrayList<>(expressions.size() * 2);
		boolean first = true;
		for (Expression e : expressions) {
			if (first)
				first = false;
			else
				concat.add(SQL.literalOf("_"));
			concat.add(e);
		}
		return Functions.count(SimpleFunction.create("DISTINCT", Collections.singletonList(SimpleFunction.create("CONCAT", concat))));
	}
	
	@Override
	public boolean isMultipleInsertSupported() {
		return false;
	}
}
