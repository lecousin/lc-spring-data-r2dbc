package net.lecousin.reactive.data.relational.postgres;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Expressions;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.SimpleFunction;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.model.PrimitiveArraysUtil;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;
import net.lecousin.reactive.data.relational.schema.Column;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;

public class PostgresSchemaDialect extends RelationalDatabaseSchemaDialect {

	@Override
	public String getName() {
		return "PostgreSQL";
	}
	
	@Override
	public boolean isCompatible(R2dbcDialect r2dbcDialect) {
		return r2dbcDialect.getClass().equals(PostgresDialect.class);
	}
	
	@Override
	public boolean isArrayColumnSupported() {
		return true;
	}
	
	@Override
	public Object convertToDataBase(Object value, PropertyMetadata property) {
		if (value != null) {
			Class<?> type = value.getClass();
			if (type.isArray()) {
				Class<?> t = type.getComponentType();
				if (t.isPrimitive() && !byte.class.equals(t) && !char.class.equals(t))
					return PrimitiveArraysUtil.primitiveArrayToObjectArray(value);
			} else if (Collection.class.isAssignableFrom(type)) {
				ParameterizedType pt = (ParameterizedType)property.getGenericType();
				Object[] array = (Object[])Array.newInstance((Class<?>)pt.getActualTypeArguments()[0], ((Collection<?>)value).size());
				return ((Collection<?>)value).toArray(array);
			}
		}
		return super.convertToDataBase(value, property);
	}
	
	@Override
	public Object convertFromDataBase(Object value, Class<?> targetType) {
		if (value != null && targetType.isArray()) {
			Class<?> t = targetType.getComponentType();
			if (t.isPrimitive() && !byte.class.equals(t) && !char.class.equals(t))
				return PrimitiveArraysUtil.objectArrayToPrimitiveArray(value, targetType.getComponentType());
		}
		return super.convertFromDataBase(value, targetType);
	}
	
	@Override
	protected void addAutoIncrement(Column col, StringBuilder sql) {
		// nothing to add
	}
	
	@Override
	protected String getArrayColumnType(Column col, Type genericType, Class<?> type, Type genericElementType, ColumnDefinition def) {
		return getColumnType(col, genericElementType, def) + "[]";
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
		int precision = def != null ? def.precision() : -1;
		if (precision < 0)
			precision = DEFAULT_TIME_PRECISION;
		return "TIMESTAMP(" + precision + ")";
	}
	
	@Override
	protected String getColumnTypeDateTimeWithTimeZone(Column col, Class<?> type, ColumnDefinition def) {
		int precision = def != null ? def.precision() : -1;
		if (precision < 0)
			precision = DEFAULT_TIME_PRECISION;
		return "TIMESTAMP(" + precision + ") WITH TIME ZONE";
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
	
	private static final String EXTRACT_DATE_TIME_FUNCTION = "EXTRACT";
	
	@Override
	public Expression applyFunctionTo(SqlFunction function, Expression expression) {
		switch (function) {
		case YEAR: return SimpleFunction.create(EXTRACT_DATE_TIME_FUNCTION, Collections.singletonList(Expressions.just("YEAR FROM " + expression)));
		case MONTH: return SimpleFunction.create(EXTRACT_DATE_TIME_FUNCTION, Collections.singletonList(Expressions.just("MONTH FROM " + expression)));
		case DAY_OF_MONTH: return SimpleFunction.create(EXTRACT_DATE_TIME_FUNCTION, Collections.singletonList(Expressions.just("DAY FROM " + expression)));
		case DAY_OF_YEAR: return SimpleFunction.create(EXTRACT_DATE_TIME_FUNCTION, Collections.singletonList(Expressions.just("DOY FROM " + expression)));
		case HOUR: return SimpleFunction.create(EXTRACT_DATE_TIME_FUNCTION, Collections.singletonList(Expressions.just("HOUR FROM " + expression)));
		case MINUTE: return SimpleFunction.create(EXTRACT_DATE_TIME_FUNCTION, Collections.singletonList(Expressions.just("MINUTE FROM " + expression)));
		case ISO_WEEK: return SimpleFunction.create(EXTRACT_DATE_TIME_FUNCTION, Collections.singletonList(Expressions.just("WEEK FROM " + expression)));
		case ISO_DAY_OF_WEEK: return SimpleFunction.create(EXTRACT_DATE_TIME_FUNCTION, Collections.singletonList(Expressions.just("ISODOW FROM " + expression)));
		case SECOND: 
			return SimpleFunction.create(EXTRACT_DATE_TIME_FUNCTION, Collections.singletonList(
				Expressions.just("SECOND FROM " + SimpleFunction.create("DATE_TRUNC", Arrays.asList(SQL.literalOf("second"), expression)))
			));
		default:
			break;
		}
		return super.applyFunctionTo(function, expression);
	}
}
