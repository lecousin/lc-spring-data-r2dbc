package net.lecousin.reactive.data.relational.h2;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.r2dbc.dialect.H2Dialect;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.schema.Column;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;

public class H2SchemaDialect extends RelationalDatabaseSchemaDialect {

	@Override
	public String getName() {
		return "H2";
	}
	
	@Override
	public boolean isCompatible(R2dbcDialect r2dbcDialect) {
		return r2dbcDialect instanceof H2Dialect;
	}
	
	@Override
	public Object convertToDataBase(Object value, RelationalPersistentProperty property) {
		if (value instanceof java.time.OffsetTime)
			return value.toString();
		if (value instanceof String) {
			ColumnDefinition def = property.findAnnotation(ColumnDefinition.class);
			if (def != null && def.min() > 0 && ((String)value).length() < def.min())
				value = StringUtils.rightPad((String)value, (int)def.min(), ' ');
		}
		return super.convertToDataBase(value, property);
	}
	
	@Override
	public Object convertFromDataBase(Object value, Class<?> targetType) {
		if (targetType.equals(java.time.OffsetTime.class))
			return java.time.OffsetTime.parse((CharSequence)value);
		return super.convertFromDataBase(value, targetType);
	}
	
	@Override
	protected String getColumnTypeFloat(Column col, Class<?> type, ColumnDefinition def) {
		return "REAL";
	}


	@Override
	protected String getColumnTypeDateTimeWithTimeZone(Column col, Class<?> type, ColumnDefinition def) {
		int precision = def != null ? def.precision() : -1;
		if (precision < 0)
			precision = DEFAULT_TIME_PRECISION;
		return "TIMESTAMP(" + precision + ") WITH TIME ZONE";
	}

	@Override
	protected String getColumnTypeTimeWithTimeZone(Column col, Class<?> type, ColumnDefinition def) {
		return "VARCHAR";
	}
	
}
