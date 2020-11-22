package net.lecousin.reactive.data.relational.mysql;

import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.dialect.SchemaGenerationDialect;
import net.lecousin.reactive.data.relational.mapping.LcReactiveDataAccessStrategy;

public class MySqlSchemaGenerationDialect extends SchemaGenerationDialect {

	public MySqlSchemaGenerationDialect(LcReactiveDataAccessStrategy dataAccess) {
		super(dataAccess);
	}
	
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
	protected void columnDefinitionDataTypeString(RelationalPersistentProperty property, StringBuilder sql) {
		ColumnDefinition def = property.findAnnotation(ColumnDefinition.class);
		if (def != null) {
			if (def.min() > 0 && def.max() == def.min()) {
				// fixed length
				if (def.max() <= 255) {
					sql.append("CHAR(").append(def.max()).append(')');
					return;
				}
			}
			if (def.max() > 255) {
				// large text
				sql.append("LONGTEXT");
			}
			if (def.max() > 0) {
				// max length
				sql.append("VARCHAR(").append(def.max()).append(')');
				return;
			}
		}
		sql.append("VARCHAR(255)");
	}

	@Override
	protected void columnDefinitionDataTypeTimeWithTimeZone(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("VARCHAR(24)");
	}
	
	@Override
	protected void columnDefinitionDataTypeDateTimeWithTimeZone(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("VARCHAR(100)");
	}
}
