package net.lecousin.reactive.data.relational.mapping;

import java.lang.reflect.Field;
import java.util.Optional;

import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

public class LcR2dbcMappingContext extends R2dbcMappingContext {

	public LcR2dbcMappingContext(NamingStrategy namingStrategy) {
		super(namingStrategy);
	}
	
	@Override
	protected <T> RelationalPersistentEntity<T> createPersistentEntity(TypeInformation<T> typeInformation) {
		if (!forceQuoteFor(typeInformation))
			return super.createPersistentEntity(typeInformation);
		setForceQuote(true);
		RelationalPersistentEntity<T> entity = super.createPersistentEntity(typeInformation);
		setForceQuote(false);
		return entity;
	}
	
	private <T> boolean forceQuoteFor(TypeInformation<T> typeInformation) {
		if (isForceQuote())
			return false;
		Table annotation = typeInformation.getRawTypeInformation().getType().getAnnotation(Table.class);
		return annotation != null && StringUtils.hasText(annotation.value());
	}
	
	@Override
	protected RelationalPersistentProperty createPersistentProperty(Property property, RelationalPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
		if (!forceQuoteFor(property))
			return super.createPersistentProperty(property, owner, simpleTypeHolder);
		setForceQuote(true);
		RelationalPersistentProperty p = super.createPersistentProperty(property, owner, simpleTypeHolder);
		setForceQuote(false);
		return p;
	}
	
	private boolean forceQuoteFor(Property property) {
		if (isForceQuote())
			return false;
		Optional<Field> field = property.getField();
		if (!field.isPresent())
			return false; // not yet supported
		Column col = field.get().getAnnotation(Column.class);
		return col != null && StringUtils.hasText(col.value());
	}

}
