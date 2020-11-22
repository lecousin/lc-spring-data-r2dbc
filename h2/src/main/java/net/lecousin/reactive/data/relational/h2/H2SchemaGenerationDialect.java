package net.lecousin.reactive.data.relational.h2;

import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import net.lecousin.reactive.data.relational.dialect.SchemaGenerationDialect;
import net.lecousin.reactive.data.relational.mapping.LcReactiveDataAccessStrategy;

public class H2SchemaGenerationDialect extends SchemaGenerationDialect {

	public H2SchemaGenerationDialect(LcReactiveDataAccessStrategy dataAccess) {
		super(dataAccess);
	}
	
	@Override
	public Object convertToDataBase(Object value) {
		if (value instanceof java.time.OffsetTime)
			return value.toString();
		return super.convertToDataBase(value);
	}
	
	@Override
	public Object convertFromDataBase(Object value, Class<?> targetType) {
		if (targetType.equals(java.time.OffsetTime.class))
			return java.time.OffsetTime.parse((CharSequence)value);
		return super.convertFromDataBase(value, targetType);
	}
	
	@Override
	protected void columnDefinitionDataTypeFloat(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("REAL");
	}


	@Override
	protected void columnDefinitionDataTypeDateTimeWithTimeZone(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("TIMESTAMP WITH TIME ZONE");
	}

	@Override
	protected void columnDefinitionDataTypeTimeWithTimeZone(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("VARCHAR");
	}
}
