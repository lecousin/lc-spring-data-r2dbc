package net.lecousin.reactive.data.relational.mapping;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.core.convert.ConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.enhance.EntityState;

public class LcEntityWriter {

	private LcMappingR2dbcConverter converter;
	private CustomConversions conversions;
	private ConversionService conversionService;
	
	public LcEntityWriter(LcMappingR2dbcConverter converter) {
		this.converter = converter;
		this.conversions = converter.getConversions();
		this.conversionService = converter.getConversionService();
	}
	
	public void write(Object source, OutboundRow sink) {
		Class<?> userClass = ClassUtils.getUserClass(source);

		Optional<Class<?>> customTarget = conversions.getCustomWriteTarget(userClass, OutboundRow.class);
		if (customTarget.isPresent()) {

			OutboundRow result = conversionService.convert(source, OutboundRow.class);
			Assert.notNull(result, "OutboundRow must not be null");
			sink.putAll(result);
			return;
		}

		RelationalPersistentEntity<?> entity = converter.getMappingContext().getRequiredPersistentEntity(userClass);
		PersistentPropertyAccessor<?> propertyAccessor = entity.getPropertyAccessor(source);

		writeProperties(sink, entity, propertyAccessor);
	}
	
	@SuppressWarnings("java:S135") // number of continue
	private void writeProperties(OutboundRow sink, RelationalPersistentEntity<?> entity, PersistentPropertyAccessor<?> accessor) {

		for (RelationalPersistentProperty property : entity) {
			if (!property.isWritable())
				continue;
			
			writeProperty(sink, property, accessor);
		}
	}
	
	public void writeProperty(OutboundRow sink, RelationalPersistentProperty property, PersistentPropertyAccessor<?> accessor) {
		Object value = accessor.getProperty(property);

		if (property.isAnnotationPresent(ForeignKey.class)) {
			RelationalPersistentEntity<?> fe = converter.getMappingContext().getRequiredPersistentEntity(property.getActualType());
			RelationalPersistentProperty idProperty = fe.getRequiredIdProperty();
			if (value != null) {
				// get the id instead of the entity
				value = EntityState.get(value, converter.getLcClient(), fe).getPersistedValue(idProperty.getName());
			}
			if (value == null) {
				sink.put(property.getColumnName(), Parameter.empty(getPotentiallyConvertedSimpleNullType(idProperty.getType())));
				return;
			}
		}

		if (value == null) {
			writeNull(sink, property);
			return;
		}
		
		value = converter.getLcClient().getSchemaDialect().convertToDataBase(value);

		if (conversions.isSimpleType(value.getClass())) {
			writeSimple(sink, value, property);
		} else {
			throw new InvalidDataAccessApiUsageException("Nested entities are not supported");
		}
	}
	
	protected void writeNull(OutboundRow sink, RelationalPersistentProperty property) {
		sink.put(property.getColumnName(), Parameter.empty(getPotentiallyConvertedSimpleNullType(property.getType())));
	}
	
	protected Class<?> getPotentiallyConvertedSimpleNullType(Class<?> type) {
		Optional<Class<?>> customTarget = conversions.getCustomWriteTarget(type);
		if (customTarget.isPresent())
			return customTarget.get();

		if (type.isEnum())
			return String.class;
		
		if (Character.class.equals(type))
			return Long.class;
		if (char[].class.equals(type))
			return String.class;

		return type;
	}
	
	protected void writeSimple(OutboundRow sink, Object value, RelationalPersistentProperty property) {
		Object converted = getPotentiallyConvertedSimpleWrite(value);
		Assert.notNull(converted, "Converted value must not be null");
		sink.put(property.getColumnName(), Parameter.from(converted));
	}
	
	/**
	 * Checks whether we have a custom conversion registered for the given value into an arbitrary simple type. Returns
	 * the converted value if so. If not, we perform special enum handling or simply return the value as is.
	 *
	 * @param value
	 * @return
	 */
	@Nullable
	protected Object getPotentiallyConvertedSimpleWrite(@Nullable Object value) {
		return getPotentiallyConvertedSimpleWrite(value, Object.class);
	}

	/**
	 * Checks whether we have a custom conversion registered for the given value into an arbitrary simple type. Returns
	 * the converted value if so. If not, we perform special enum handling or simply return the value as is.
	 *
	 * @param value
	 * @return
	 */
	@Nullable
	protected Object getPotentiallyConvertedSimpleWrite(@Nullable Object value, Class<?> typeHint) {
		if (value == null)
			return null;

		if (Object.class != typeHint && conversionService.canConvert(value.getClass(), typeHint)) {
			value = conversionService.convert(value, typeHint);
			if (value == null)
				return null;
		}

		if (value instanceof Number) {
			if (value instanceof Double || value instanceof Float)
				return Double.valueOf(((Number)value).doubleValue());
			if (!(value instanceof BigDecimal))
				return Long.valueOf(((Number)value).longValue());
		} else if (value instanceof Character) {
			return Long.valueOf((Character)value);
		} else if (char[].class.equals(value.getClass())) {
			return new String((char[])value);
		}
		
		Optional<Class<?>> customTarget = conversions.getCustomWriteTarget(value.getClass());

		if (customTarget.isPresent())
			return conversionService.convert(value, customTarget.get());

		if (Enum.class.isAssignableFrom(value.getClass()))
			return ((Enum<?>) value).name();
		
		return value;
	}
	
}
