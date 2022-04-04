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
package net.lecousin.reactive.data.relational.mapping;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.metadata.EntityMetadata;
import net.lecousin.reactive.data.relational.model.metadata.PropertyMetadata;

/**
 * Write properties to an OutboundRow.
 * 
 * @author Guillaume Le Cousin
 *
 */
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

		EntityMetadata entity = converter.getLcClient().getRequiredEntity(userClass);
		PersistentPropertyAccessor<?> propertyAccessor = entity.getSpringMetadata().getPropertyAccessor(source);

		writeProperties(sink, entity, propertyAccessor);
	}
	
	@SuppressWarnings("java:S135") // number of continue
	private void writeProperties(OutboundRow sink, EntityMetadata entity, PersistentPropertyAccessor<?> accessor) {

		for (PropertyMetadata property : entity.getPersistentProperties()) {
			if (!property.isWritable())
				continue;
			
			writeProperty(sink, property, accessor);
		}
	}
	
	public void writeProperty(OutboundRow sink, PropertyMetadata property, PersistentPropertyAccessor<?> accessor) {
		Object value = accessor.getProperty(property.getRequiredSpringProperty());

		if (property.isForeignKey()) {
			EntityMetadata fe = converter.getLcClient().getRequiredEntity(property.getType());
			PropertyMetadata idProperty = fe.getRequiredIdProperty();
			if (value != null) {
				// get the id instead of the entity
				value = EntityState.get(value, fe).getPersistedValue(idProperty.getName());
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
		
		value = converter.getLcClient().getSchemaDialect().convertToDataBase(value, property);

		writeSimple(sink, value, property);
	}
	
	protected void writeNull(OutboundRow sink, PropertyMetadata property) {
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
	
	protected void writeSimple(OutboundRow sink, Object value, PropertyMetadata property) {
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
