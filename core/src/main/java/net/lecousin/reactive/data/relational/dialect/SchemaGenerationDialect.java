package net.lecousin.reactive.data.relational.dialect;

import java.math.BigDecimal;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.SqlIdentifier;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import net.lecousin.reactive.data.relational.mapping.LcReactiveDataAccessStrategy;
import net.lecousin.reactive.data.relational.model.ModelUtils;

/**
 * Base class to generate schema.
 */
@SuppressWarnings({
	"java:S1172", "unused" // unused parameters are present because implementations may need them 
})
public abstract class SchemaGenerationDialect {
	
	protected MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext;
	protected ReactiveDataAccessStrategy dataAccess;
	protected R2dbcDialect dialect;
	protected IdentifierProcessing identifierProcessing;
	
	public SchemaGenerationDialect(LcReactiveDataAccessStrategy dataAccess) {
		this.dataAccess = dataAccess;
		this.mappingContext = dataAccess.getMappingContext();
		this.dialect = dataAccess.getDialect();
		this.identifierProcessing = dialect.getIdentifierProcessing();
	}
	
	public Object convertToDataBase(Object value) {
		return value;
	}
	
	public Object convertFromDataBase(Object value, Class<?> targetType) {
		return value;
	}
	
	protected String toSql(SqlIdentifier identifier) {
		if (identifierProcessing != null)
			return identifier.toSql(identifierProcessing);
		return dataAccess.toSql(identifier);
	}
	
	public void dropTable(RelationalPersistentEntity<?> entity, boolean ifExists, StringBuilder sql) {
		sql.append("DROP TABLE ");
		if (ifExists)
			sql.append("IF EXISTS ");
		sql.append(dataAccess.toSql(entity.getTableName()));
	}
	
	public void createTable(RelationalPersistentEntity<?> entity, StringBuilder sql) {
		sql.append("CREATE TABLE ").append(toSql(entity.getTableName()));
		sql.append(" (");
		boolean first = true;
		for (RelationalPersistentProperty property : entity) {
			if (first)
				first = false;
			else
				sql.append(", ");
			sql.append(toSql(property.getColumnName()));
			sql.append(' ');
			columnDefinition(property, sql);
		}
		sql.append(')');
	}
	
	protected void columnDefinition(RelationalPersistentProperty property, StringBuilder sql) {
		if (property.isAnnotationPresent(GeneratedValue.class)) {
			columnDefinitionGenerated(property, sql);
		} else {
			columnDefinitionDataType(property, sql);
			if (!ModelUtils.isNullable(property))
				columnDefinitionNotNull(property, sql);
		}
		if (property.isIdProperty())
			columnDefinitionPrimaryKey(property, sql);
	}
	
	@SuppressWarnings("java:S3776") // complexity
	protected void columnDefinitionDataType(RelationalPersistentProperty property, StringBuilder sql) {
		Class<?> type;
		if (property.isAnnotationPresent(ForeignKey.class)) {
			RelationalPersistentEntity<?> entity = mappingContext.getPersistentEntity(property.getType());
			if (entity == null)
				throw new SchemaGenerationException(property, "a foreign key must have the type of an entity");
			RelationalPersistentProperty idProperty = entity.getIdProperty();
			if (idProperty == null)
				throw new SchemaGenerationException(property, "a foreign key must have the type of an entity containing an ID property");
			type = idProperty.getType();
		} else {
			type = property.getType();
		}
		if (boolean.class.equals(type) || Boolean.class.equals(type))
			columnDefinitionDataTypeBoolean(property, sql);
		else if (byte.class.equals(type) || Byte.class.equals(type))
			columnDefinitionDataTypeByte(property, sql);
		else if (short.class.equals(type) || Short.class.equals(type))
			columnDefinitionDataTypeShort(property, sql);
		else if (int.class.equals(type) || Integer.class.equals(type))
			columnDefinitionDataTypeInteger(property, sql);
		else if (long.class.equals(type) || Long.class.equals(type))
			columnDefinitionDataTypeLong(property, sql);
		else if (float.class.equals(type) || Float.class.equals(type))
			columnDefinitionDataTypeFloat(property, sql);
		else if (double.class.equals(type) || Double.class.equals(type))
			columnDefinitionDataTypeDouble(property, sql);
		else if (BigDecimal.class.equals(type))
			columnDefinitionDataTypeBigDecimal(property, sql);
		else if (String.class.equals(type) || char[].class.equals(type))
			columnDefinitionDataTypeString(property, sql);
		else if (char.class.equals(type) || Character.class.equals(type))
			columnDefinitionDataTypeChar(property, sql);
		else if (java.time.LocalDate.class.equals(type))
			columnDefinitionDataTypeDate(property, sql);
		else if (java.time.LocalTime.class.equals(type))
			columnDefinitionDataTypeTime(property, sql);
		else if (java.time.OffsetTime.class.equals(type))
			columnDefinitionDataTypeTimeWithTimeZone(property, sql);
		else if (java.time.LocalDateTime.class.equals(type))
			columnDefinitionDataTypeDateTime(property, sql);
		else if (java.time.ZonedDateTime.class.equals(type))
			columnDefinitionDataTypeDateTimeWithTimeZone(property, sql);
		else if (java.time.Instant.class.equals(type))
			columnDefinitionDataTypeTimestamp(property, sql);
		else
			throw new SchemaGenerationException(property, "type not supported: " + type.getName());
	}
	
	protected void columnDefinitionDataTypeBoolean(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("BOOLEAN");
	}
	
	protected void columnDefinitionDataTypeByte(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("TINYINT");
	}
	
	protected void columnDefinitionDataTypeShort(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("SMALLINT");
	}
	
	protected void columnDefinitionDataTypeInteger(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("INT");
	}
	
	protected void columnDefinitionDataTypeLong(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("BIGINT");
	}
	
	protected void columnDefinitionDataTypeFloat(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("FLOAT");
	}
	
	protected void columnDefinitionDataTypeDouble(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("DOUBLE");
	}
	
	protected void columnDefinitionDataTypeBigDecimal(RelationalPersistentProperty property, StringBuilder sql) {
		int precision = ColumnDefinition.DEFAULT_PRECISION;
		int scale = ColumnDefinition.DEFAULT_SCALE;
		ColumnDefinition def = property.findAnnotation(ColumnDefinition.class);
		if (def != null) {
			precision = def.precision();
			scale = def.scale();
		}
		sql.append("DECIMAL(").append(precision).append(',').append(scale).append(')');
	}
	
	protected void columnDefinitionDataTypeChar(RelationalPersistentProperty property, StringBuilder sql) {
		columnDefinitionDataTypeShort(property, sql);
	}
	
	protected void columnDefinitionDataTypeString(RelationalPersistentProperty property, StringBuilder sql) {
		ColumnDefinition def = property.findAnnotation(ColumnDefinition.class);
		if (def != null) {
			if (def.max() > Integer.MAX_VALUE) {
				// large text
				sql.append("CLOB(").append(def.max()).append(')');
			}
			if (def.min() > 0 && def.max() == def.min()) {
				// fixed length
				sql.append("CHAR(").append(def.max()).append(')');
				return;
			}
			if (def.max() > 0) {
				// max length
				sql.append("VARCHAR(").append(def.max()).append(')');
				return;
			}
		}
		sql.append("VARCHAR");
	}

	protected void columnDefinitionDataTypeTimestamp(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("TIMESTAMP");
	}

	protected void columnDefinitionDataTypeDate(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("DATE");
	}

	protected void columnDefinitionDataTypeTime(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("TIME");
	}

	protected void columnDefinitionDataTypeTimeWithTimeZone(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("TIME WITH TIME ZONE");
	}

	protected void columnDefinitionDataTypeDateTime(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("DATETIME");
	}

	protected void columnDefinitionDataTypeDateTimeWithTimeZone(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append("DATETIME WITH TIME ZONE");
	}
	
	protected void columnDefinitionNotNull(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append(" NOT NULL");
	}
	
	protected void columnDefinitionGenerated(RelationalPersistentProperty property, StringBuilder sql) {
		columnDefinitionDataType(property, sql);
		columnDefinitionNotNull(property, sql);
		sql.append(" AUTO_INCREMENT");
	}
	
	protected void columnDefinitionPrimaryKey(RelationalPersistentProperty property, StringBuilder sql) {
		sql.append(" PRIMARY KEY");
	}
	
}
