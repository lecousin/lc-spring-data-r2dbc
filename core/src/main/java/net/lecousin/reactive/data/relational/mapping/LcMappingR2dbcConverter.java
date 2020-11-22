package net.lecousin.reactive.data.relational.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.SettableValue;
import org.springframework.data.r2dbc.support.ArrayUtils;
import org.springframework.data.relational.core.conversion.BasicRelationalConverter;
import org.springframework.data.relational.core.conversion.RelationalConverter;
import org.springframework.data.relational.core.dialect.ArrayColumns;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.ModelUtils;

public class LcMappingR2dbcConverter extends BasicRelationalConverter implements R2dbcConverter {

	private static final Log logger = LogFactory.getLog(LcMappingR2dbcConverter.class);
	
	private LcReactiveDataRelationalClient client;
	
	public LcMappingR2dbcConverter(
			MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> context,
			CustomConversions conversions,
			LcReactiveDataRelationalClient client
		) {
		super(context, conversions);
		this.client = client;
		client.setMapper(this);
	}

	// ----------------------------------
	// Entity reading
	// ----------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.EntityReader#read(java.lang.Class, S)
	 */
	@Override
	public <R> R read(Class<R> type, Row row) {
		return read(type, row, null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.r2dbc.convert.R2dbcConverter#read(java.lang.Class, io.r2dbc.spi.Row, io.r2dbc.spi.RowMetadata)
	 */
	@Override
	public <R> R read(Class<R> type, Row row, @Nullable RowMetadata metadata) {
		return read(type, row, metadata, null);
	}
	
	public <R> R read(Class<R> type, Row row, @Nullable RowMetadata metadata, @Nullable ResultMappingContext resultContext) {

		TypeInformation<? extends R> typeInfo = ClassTypeInformation.from(type);
		Class<? extends R> rawType = typeInfo.getType();

		if (Row.class.isAssignableFrom(rawType)) {
			return type.cast(row);
		}

		if (getConversions().hasCustomReadTarget(Row.class, rawType)
				&& getConversionService().canConvert(Row.class, rawType)) {
			return getConversionService().convert(row, rawType);
		}

		return read(getRequiredPersistentEntity(type), row, metadata, resultContext);
	}

	private <R> R read(RelationalPersistentEntity<R> entity, Row row, @Nullable RowMetadata metadata, @Nullable ResultMappingContext resultContext) {
		if (logger.isDebugEnabled())
			logger.debug("Read row <" + row + "> into " + entity.getName());
		
		R result = getInstance(row, metadata, "", entity, resultContext);
		EntityState state = EntityState.get(result, client, entity);

		if (entity.requiresPropertyPopulation()) {
			ConvertingPropertyAccessor<R> propertyAccessor = new ConvertingPropertyAccessor<>(
					entity.getPropertyAccessor(result), getConversionService());

			for (RelationalPersistentProperty property : entity) {

				if (entity.isConstructorArgument(property)) {
					continue;
				}

				Object value = readFrom(row, metadata, property, "", result, resultContext);

				if (value != null) {
					propertyAccessor.setProperty(property, value);
				}
			}
		}
		state.loaded(result);

		return result;
	}

	/**
	 * Read a single value or a complete Entity from the {@link Row} passed as an argument.
	 *
	 * @param row the {@link Row} to extract the value from. Must not be {@literal null}.
	 * @param metadata the {@link RowMetadata}. Can be {@literal null}.
	 * @param property the {@link RelationalPersistentProperty} for which the value is intended. Must not be
	 *          {@literal null}.
	 * @param prefix to be used for all column names accessed by this method. Must not be {@literal null}.
	 * @return the value read from the {@link Row}. May be {@literal null}.
	 */
	private Object readFrom(Row row, @Nullable RowMetadata metadata, RelationalPersistentProperty property,
			String prefix, Object instance, @Nullable ResultMappingContext resultContext) {

		try {

			if (property.isEntity()) {
				return readEntityFrom(row, metadata, property, instance, resultContext);
			}

			String identifier = prefix + property.getColumnName();
			if (metadata != null && !metadata.getColumnNames().contains(identifier)) {
				return null;
			}

			Object value = row.get(identifier);
			return readValue(value, property.getTypeInformation());

		} catch (Exception error) {
			throw new MappingException(String.format("Could not read property %s from result set!", property), error);
		}
	}

	@Override
	public Object readValue(@Nullable Object value, TypeInformation<?> type) {

		if (null == value) {
			return null;
		}
		
		value = client.getSchemaDialect().convertFromDataBase(value, type.getType());

		if (getConversions().hasCustomReadTarget(value.getClass(), type.getType())) {
			return getConversionService().convert(value, type.getType());
		} else if ((value instanceof String) && char[].class.equals(type.getType())) {
			return ((String)value).toCharArray();
		} else if (value instanceof Collection || value.getClass().isArray()) {
			return readCollectionOrArray(asCollection(value), type);
		} else {
			return getPotentiallyConvertedSimpleRead2(value, type.getType());
		}
	}

	/**
	 * Reads the given value into a collection of the given {@link TypeInformation}.
	 *
	 * @param source must not be {@literal null}.
	 * @param targetType must not be {@literal null}.
	 * @return the converted {@link Collection} or array, will never be {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	private Object readCollectionOrArray(Collection<?> source, TypeInformation<?> targetType) {

		Assert.notNull(targetType, "Target type must not be null!");

		Class<?> collectionType = targetType.isSubTypeOf(Collection.class) //
				? targetType.getType() //
				: List.class;

		TypeInformation<?> componentType = targetType.getComponentType() != null //
				? targetType.getComponentType() //
				: ClassTypeInformation.OBJECT;
		Class<?> rawComponentType = componentType.getType();

		Collection<Object> items = targetType.getType().isArray() //
				? new ArrayList<>(source.size()) //
				: CollectionFactory.createCollection(collectionType, rawComponentType, source.size());

		if (source.isEmpty()) {
			return getPotentiallyConvertedSimpleRead2(items, targetType.getType());
		}

		for (Object element : source) {

			if (!Object.class.equals(rawComponentType) && element instanceof Collection && !rawComponentType.isArray() && !ClassUtils.isAssignable(Iterable.class, rawComponentType)) {
				throw new MappingException(String.format(
						"Cannot convert %1$s of type %2$s into an instance of %3$s! Implement a custom Converter<%2$s, %3$s> and register it with the CustomConversions",
						element, element.getClass(), rawComponentType));
			}
			if (element instanceof List) {
				items.add(readCollectionOrArray((Collection<Object>) element, componentType));
			} else {
				items.add(getPotentiallyConvertedSimpleRead2(element, rawComponentType));
			}
		}

		return getPotentiallyConvertedSimpleRead2(items, targetType.getType());
	}

	/**
	 * Checks whether we have a custom conversion for the given simple object. Converts the given value if so, applies
	 * {@link Enum} handling or returns the value as is.
	 *
	 * @param value
	 * @param target must not be {@literal null}.
	 * @return
	 */
	@Nullable
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object getPotentiallyConvertedSimpleRead2(@Nullable Object value, @Nullable Class<?> target) {

		if (value == null || target == null || ClassUtils.isAssignableValue(target, value)) {
			return value;
		}

		if (getConversions().hasCustomReadTarget(value.getClass(), target)) {
			return getConversionService().convert(value, target);
		}

		if (Enum.class.isAssignableFrom(target)) {
			return Enum.valueOf((Class<Enum>) target, value.toString());
		}

		return getConversionService().convert(value, target);
	}

	@SuppressWarnings({"unchecked", "java:S2583"}) // false positive
	private <S> S readEntityFrom(Row row, RowMetadata metadata, RelationalPersistentProperty property, Object parentInstance, ResultMappingContext resultContext) {
		RelationalPersistentEntity<?> entity = getMappingContext().getRequiredPersistentEntity(property.getActualType());

		if (property.isAnnotationPresent(ForeignKey.class))
			return readForeignKeyEntity(row, metadata, property, parentInstance, resultContext, entity);
		
		String prefix = property.getName() + "_";

		if (readFrom(row, metadata, entity.getRequiredIdProperty(), prefix, null, resultContext) == null) {
			return null;
		}

		Object instance = getInstance(row, metadata, prefix, entity, resultContext);

		if (entity.requiresPropertyPopulation()) {
			PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(instance);
			ConvertingPropertyAccessor<?> propertyAccessor = new ConvertingPropertyAccessor<>(accessor,
					getConversionService());

			for (RelationalPersistentProperty p : entity) {
				if (!entity.isConstructorArgument(property)) {
					propertyAccessor.setProperty(p, readFrom(row, metadata, p, prefix, instance, resultContext));
				}
			}
		}

		return (S) instance;
	}
	
	@SuppressWarnings({"unchecked", "java:S2583"}) // false positive
	private <S> S readForeignKeyEntity(Row row, RowMetadata metadata, RelationalPersistentProperty property, Object parentInstance, ResultMappingContext resultContext, RelationalPersistentEntity<?> entity) {
		String identifier = /*prefix + */ property.getColumnName().toString();
		if (metadata != null && !metadata.getColumnNames().contains(identifier)) {
			return null;
		}

		Object value = row.get(identifier);
		if (value == null)
			return null; // foreign key is null
		Object instance = null;
		if (resultContext != null) {
			instance = resultContext.getEntityCache().getCachedInstance(property.getActualType(), value);
		}
		if (instance == null) {
			instance = getInstance(row, metadata, "", entity, resultContext);
		}
		entity.getPropertyAccessor(instance).setProperty(entity.getRequiredIdProperty(), value);
		ModelUtils.setReverseLink(instance, parentInstance, property);
		EntityState state = EntityState.get(instance, client, entity);
		state.lazyLoaded();
		return (S) instance;
	}

	@SuppressWarnings("unchecked")
	private <S> S getInstance(Row row, @Nullable RowMetadata rowMetadata, String prefix,
			RelationalPersistentEntity<S> entity, @Nullable ResultMappingContext resultContext) {

		RelationalPersistentProperty idProperty = null;
		Object id = null;
		
		if (resultContext != null) {
			idProperty = entity.getIdProperty();
			if (idProperty != null) {
				try {
					id = row.get(idProperty.getName());
				} catch (Exception e) {
					// not available
				}
				if (id != null) {
					Object instance = resultContext.getEntityCache().getCachedInstance(entity.getType(), id);
					if (instance != null)
						return (S) instance;
				}
			}
		}
		
		RowParameterValueProvider rowParameterValueProvider = new RowParameterValueProvider(row, rowMetadata, entity, this,
				prefix);

		S instance = createInstance(entity, rowParameterValueProvider::getParameterValue);
		
		if (resultContext != null && id != null)
			resultContext.getEntityCache().setCachedInstance(entity.getType(), id, instance);
		
		return instance;
	}

	// ----------------------------------
	// Entity writing
	// ----------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.EntityWriter#write(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void write(Object source, OutboundRow sink) {

		Class<?> userClass = ClassUtils.getUserClass(source);

		Optional<Class<?>> customTarget = getConversions().getCustomWriteTarget(userClass, OutboundRow.class);
		if (customTarget.isPresent()) {

			OutboundRow result = getConversionService().convert(source, OutboundRow.class);
			Assert.notNull(result, "OutboundRow must not be null");
			sink.putAll(result);
			return;
		}

		writeInternal(source, sink, userClass);
	}

	private void writeInternal(Object source, OutboundRow sink, Class<?> userClass) {

		RelationalPersistentEntity<?> entity = getRequiredPersistentEntity(userClass);
		PersistentPropertyAccessor<?> propertyAccessor = entity.getPropertyAccessor(source);

		writeProperties(sink, entity, propertyAccessor);
	}

	@SuppressWarnings("java:S135") // number of continue
	private void writeProperties(OutboundRow sink, RelationalPersistentEntity<?> entity,
			PersistentPropertyAccessor<?> accessor) {

		for (RelationalPersistentProperty property : entity) {

			if (!property.isWritable()) {
				continue;
			}

			Object value = accessor.getProperty(property);

			if (value == null) {
				writeNullInternal(sink, property);
				continue;
			}

			if (getConversions().isSimpleType(value.getClass())) {
				writeSimpleInternal(sink, value, property);
			} else {
				writePropertyInternal(sink, value, property);
			}
		}
	}

	private void writeSimpleInternal(OutboundRow sink, Object value, RelationalPersistentProperty property) {
		Object converted = getPotentiallyConvertedSimpleWrite2(value);
		Assert.notNull(converted, "Converted value must not be null");
		sink.put(property.getColumnName(), SettableValue.from(converted));
	}

	private void writePropertyInternal(OutboundRow sink, Object value, RelationalPersistentProperty property) {

		TypeInformation<?> valueType = ClassTypeInformation.from(value.getClass());

		if (valueType.isCollectionLike()) {

			if (valueType.getActualType() != null && valueType.getRequiredActualType().isCollectionLike()) {

				// pass-thru nested collections
				writeSimpleInternal(sink, value, property);
				return;
			}

			List<Object> collectionInternal = createCollection(asCollection(value), property);
			sink.put(property.getColumnName(), SettableValue.from(collectionInternal));
			return;
		}

		throw new InvalidDataAccessApiUsageException("Nested entities are not supported");
	}

	/**
	 * Writes the given {@link Collection} using the given {@link RelationalPersistentProperty} information.
	 *
	 * @param collection must not be {@literal null}.
	 * @param property must not be {@literal null}.
	 * @return
	 */
	protected List<Object> createCollection(Collection<?> collection, RelationalPersistentProperty property) {
		return writeCollectionInternal(collection, property.getTypeInformation(), new ArrayList<>());
	}

	/**
	 * Populates the given {@link Collection sink} with converted values from the given {@link Collection source}.
	 *
	 * @param source the collection to create a {@link Collection} for, must not be {@literal null}.
	 * @param type the {@link TypeInformation} to consider or {@literal null} if unknown.
	 * @param sink the {@link Collection} to write to.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Object> writeCollectionInternal(Collection<?> source, @Nullable TypeInformation<?> type,
			Collection<?> sink) {

		TypeInformation<?> componentType = null;

		List<Object> collection = sink instanceof List ? (List<Object>) sink : new ArrayList<>(sink);

		if (type != null) {
			componentType = type.getComponentType();
		}

		for (Object element : source) {

			Class<?> elementType = element == null ? null : element.getClass();

			if (elementType == null || getConversions().isSimpleType(elementType)) {
				collection.add(getPotentiallyConvertedSimpleWrite(element,
						componentType != null ? componentType.getType() : Object.class));
			} else if (element instanceof Collection || elementType.isArray()) {
				collection.add(writeCollectionInternal(asCollection(element), componentType, new ArrayList<>()));
			} else {
				throw new InvalidDataAccessApiUsageException("Nested entities are not supported");
			}
		}

		return collection;
	}

	private void writeNullInternal(OutboundRow sink, RelationalPersistentProperty property) {

		sink.put(property.getColumnName(), SettableValue.empty(getPotentiallyConvertedSimpleNullType(property.getType())));
	}

	private Class<?> getPotentiallyConvertedSimpleNullType(Class<?> type) {

		Optional<Class<?>> customTarget = getConversions().getCustomWriteTarget(type);

		if (customTarget.isPresent()) {
			return customTarget.get();

		}

		if (type.isEnum()) {
			return String.class;
		}

		return type;
	}

	/**
	 * Checks whether we have a custom conversion registered for the given value into an arbitrary simple type. Returns
	 * the converted value if so. If not, we perform special enum handling or simply return the value as is.
	 *
	 * @param value
	 * @return
	 */
	@Nullable
	private Object getPotentiallyConvertedSimpleWrite2(@Nullable Object value) {
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
	private Object getPotentiallyConvertedSimpleWrite(@Nullable Object value, Class<?> typeHint) {

		if (value == null) {
			return null;
		}

		if (Object.class != typeHint && getConversionService().canConvert(value.getClass(), typeHint)) {
			value = getConversionService().convert(value, typeHint);
			if (value == null) {
				return null;
			}
		}

		Optional<Class<?>> customTarget = getConversions().getCustomWriteTarget(value.getClass());

		if (customTarget.isPresent()) {
			return getConversionService().convert(value, customTarget.get());
		}

		return Enum.class.isAssignableFrom(value.getClass()) ? ((Enum<?>) value).name() : value;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.r2dbc.convert.R2dbcConverter#getArrayValue(org.springframework.data.r2dbc.dialect.ArrayColumns, org.springframework.data.relational.core.mapping.RelationalPersistentProperty, java.lang.Object)
	 */
	@Override
	public Object getArrayValue(ArrayColumns arrayColumns, RelationalPersistentProperty property, Object value) {

		Class<?> actualType = null;
		if (value instanceof Collection) {
			actualType = CollectionUtils.findCommonElementType((Collection<?>) value);
		} else if (value.getClass().isArray()) {
			actualType = value.getClass().getComponentType();
		}

		if (actualType == null) {
			actualType = property.getActualType();
		}

		Class<?> targetType = arrayColumns.getArrayType(actualType);

		if (!property.isArray() || !targetType.isAssignableFrom(value.getClass())) {

			int depth = value.getClass().isArray() ? ArrayUtils.getDimensionDepth(value.getClass()) : 1;
			Class<?> targetArrayType = ArrayUtils.getArrayClass(targetType, depth);
			return getConversionService().convert(value, targetArrayType);
		}

		return value;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.r2dbc.convert.R2dbcConverter#getTargetType(Class)
	 */
	@Override
	public Class<?> getTargetType(Class<?> valueType) {

		Optional<Class<?>> writeTarget = getConversions().getCustomWriteTarget(valueType);

		return writeTarget.orElseGet(() -> Enum.class.isAssignableFrom(valueType) ? String.class : valueType);
	}

	// ----------------------------------
	// Id handling
	// ----------------------------------

	/**
	 * Returns a {@link java.util.function.Function} that populates the id property of the {@code object} from a
	 * {@link Row}.
	 *
	 * @param object must not be {@literal null}.
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> BiFunction<Row, RowMetadata, T> populateIdIfNecessary(T object) {

		Assert.notNull(object, "Entity object must not be null!");

		Class<?> userClass = ClassUtils.getUserClass(object);
		RelationalPersistentEntity<?> entity = getMappingContext().getRequiredPersistentEntity(userClass);

		return (row, metadata) -> {

			PersistentPropertyAccessor<?> propertyAccessor = entity.getPropertyAccessor(object);
			RelationalPersistentProperty idProperty = entity.getRequiredIdProperty();

			if (propertyAccessor.getProperty(idProperty) != null) {
				return object;
			}

			return potentiallySetId(row, metadata, propertyAccessor, idProperty) //
					? (T) propertyAccessor.getBean() //
					: object;
		};
	}

	private boolean potentiallySetId(Row row, RowMetadata metadata, PersistentPropertyAccessor<?> propertyAccessor,
			RelationalPersistentProperty idProperty) {

		Collection<String> columns = metadata.getColumnNames();
		Object generatedIdValue = null;
		String idColumnName = idProperty.getColumnName().getReference(IdentifierProcessing.NONE);

		if (columns.contains(idColumnName)) {
			generatedIdValue = row.get(idColumnName);
		} else if (columns.size() == 1) {

			String key = columns.iterator().next();
			generatedIdValue = row.get(key);
		}

		if (generatedIdValue == null) {
			return false;
		}

		ConversionService conversionService = getConversionService();
		propertyAccessor.setProperty(idProperty, conversionService.convert(generatedIdValue, idProperty.getType()));

		return true;
	}

	@SuppressWarnings("unchecked")
	private <R> RelationalPersistentEntity<R> getRequiredPersistentEntity(Class<R> type) {
		return (RelationalPersistentEntity<R>) getMappingContext().getRequiredPersistentEntity(type);
	}

	/**
	 * Returns given object as {@link Collection}. Will return the {@link Collection} as is if the source is a
	 * {@link Collection} already, will convert an array into a {@link Collection} or simply create a single element
	 * collection for everything else.
	 *
	 * @param source
	 * @return
	 */
	private static Collection<?> asCollection(Object source) {

		if (source instanceof Collection) {
			return (Collection<?>) source;
		}

		return source.getClass().isArray() ? CollectionUtils.arrayToList(source) : Collections.singleton(source);
	}

	private static class RowParameterValueProvider implements ParameterValueProvider<RelationalPersistentProperty> {

		private final Row resultSet;
		private final RowMetadata metadata;
		private final RelationalPersistentEntity<?> entity;
		private final RelationalConverter converter;
		private final String prefix;

		public RowParameterValueProvider(Row resultSet, RowMetadata metadata, RelationalPersistentEntity<?> entity,
				RelationalConverter converter, String prefix) {
			this.resultSet = resultSet;
			this.metadata = metadata;
			this.entity = entity;
			this.converter = converter;
			this.prefix = prefix;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.model.ParameterValueProvider#getParameterValue(org.springframework.data.mapping.PreferredConstructor.Parameter)
		 */
		@Override
		@Nullable
		@SuppressWarnings("java:S2583") // false positive
		public <T> T getParameterValue(Parameter<T, RelationalPersistentProperty> parameter) {
			String paramName = parameter.getName();
			Assert.notNull(paramName, "Parameter name must not be null");
			RelationalPersistentProperty property = this.entity.getRequiredPersistentProperty(paramName);

			String reference = property.getColumnName().getReference(IdentifierProcessing.NONE);
			String column = this.prefix.isEmpty() ? reference : this.prefix + reference;

			try {

				if (this.metadata != null && !this.metadata.getColumnNames().contains(column)) {
					return null;
				}

				Object value = this.resultSet.get(column);

				if (value == null) {
					return null;
				}

				Class<T> type = parameter.getType().getType();

				if (type.isInstance(value)) {
					return type.cast(value);
				}
				return this.converter.getConversionService().convert(value, type);
			} catch (Exception error) {
				throw new MappingException(String.format("Couldn't read column %s from Row.", column), error);
			}
		}
	}
}
