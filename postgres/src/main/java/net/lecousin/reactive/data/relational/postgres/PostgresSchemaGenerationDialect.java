package net.lecousin.reactive.data.relational.postgres;

import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import net.lecousin.reactive.data.relational.dialect.SchemaGenerationDialect;
import net.lecousin.reactive.data.relational.mapping.LcReactiveDataAccessStrategy;

public class PostgresSchemaGenerationDialect extends SchemaGenerationDialect {

	public PostgresSchemaGenerationDialect(LcReactiveDataAccessStrategy dataAccess) {
		super(dataAccess);
	}
	
	@Override
	protected void columnDefinitionGenerated(RelationalPersistentProperty property, StringBuilder sql) {
		Class<?> type = property.getType();
		if (byte.class.equals(type) || Byte.class.equals(type) || short.class.equals(type) || Short.class.equals(type))
			sql.append("SMALLSERIAL");
		else if (int.class.equals(type) || Integer.class.equals(type))
			sql.append("SERIAL");
		else
			sql.append("BIGSERIAL");
	}

	@Override
	protected void columnDefinitionDataTypeByte(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("SMALLINT");
	}
	
	@Override
	protected void columnDefinitionDataTypeInteger(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("INTEGER");
	}
	
	@Override
	protected void columnDefinitionDataTypeFloat(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("REAL");
	}
	
	@Override
	protected void columnDefinitionDataTypeDouble(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("DOUBLE PRECISION");
	}
	
	@Override
	protected void columnDefinitionDataTypeDateTime(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("TIMESTAMP");
	}
	
	@Override
	protected void columnDefinitionDataTypeDateTimeWithTimeZone(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("TIMESTAMP WITH TIME ZONE");
	}
	
}
