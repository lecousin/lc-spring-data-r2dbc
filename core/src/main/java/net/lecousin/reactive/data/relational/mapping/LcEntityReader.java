package net.lecousin.reactive.data.relational.mapping;

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.relational.core.conversion.BasicRelationalConverter;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.enhance.EntityState;
import net.lecousin.reactive.data.relational.model.EntityCache;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.model.PropertiesSource;

public class LcEntityReader {
	
	private static final Log logger = LogFactory.getLog(LcEntityReader.class);
	
	private CustomConversions conversions;
	private ConversionService conversionService;
	private EntityCache cache;
	private LcReactiveDataRelationalClient client;

	public LcEntityReader(
		@Nullable EntityCache cache,
		@Nullable CustomConversions conversions,
		LcReactiveDataRelationalClient client
	) {
		this.cache = cache != null ? cache : new EntityCache();
		R2dbcConverter converter = client.getDataAccess().getConverter();
		if (conversions != null) {
			this.conversions = conversions;
		} else if (converter instanceof BasicRelationalConverter) {
			this.conversions = ((BasicRelationalConverter)converter).getConversions();
		} else {
			throw new IllegalArgumentException("No conversions");
		}
		this.conversionService = converter.getConversionService();
		this.client = client;
	}
	
	public LcEntityReader(
		@Nullable EntityCache cache,
		LcMappingR2dbcConverter converter
	) {
		this(cache, converter.getConversions(), converter.getLcClient());
	}
	
	public EntityCache getCache() {
		return cache;
	}

	@SuppressWarnings("unchecked")
	public <T> T read(Class<T> type, PropertiesSource source) {
		TypeInformation<? extends T> typeInfo = ClassTypeInformation.from(type);
		Class<? extends T> rawType = typeInfo.getType();
		
		Class<?> sourceType = source.getSource().getClass();

		if (rawType.isAssignableFrom(sourceType)) {
			return (T) source.getSource();
		}

		if (conversions.hasCustomReadTarget(sourceType, rawType) && conversionService.canConvert(sourceType, rawType)) {
			return conversionService.convert(source.getSource(), rawType);
		}
		
		return read((RelationalPersistentEntity<T>) client.getMappingContext().getRequiredPersistentEntity(type), source);
	}
	
	public <T> T read(RelationalPersistentEntity<T> entityType, PropertiesSource source) {
		if (logger.isDebugEnabled())
			logger.debug("Read <" + source.getSource() + "> into " + entityType.getName());
		
		T result = getOrCreateInstance(entityType, source);
		EntityState state = EntityState.get(result, client, entityType);

		if (entityType.requiresPropertyPopulation()) {
			ConvertingPropertyAccessor<T> propertyAccessor = new ConvertingPropertyAccessor<>(entityType.getPropertyAccessor(result), conversionService);

			for (RelationalPersistentProperty property : entityType) {

				if (entityType.isConstructorArgument(property))
					continue;

				MutableObject<Object> value = readProperty(property, source, result, property.getTypeInformation().getType());

				if (value != null) {
					propertyAccessor.setProperty(property, value.getValue());
				} else if (property.isIdProperty() || ModelUtils.isPropertyPartOfCompositeId(property)) {
					throw new MappingException("Property " + entityType.getName() + "." + property.getName() + " must be returned by the query to be mapped");
				}
			}
		}
		state.loaded(result); 

		return result;
	}
	
	protected MutableObject<Object> readProperty(RelationalPersistentProperty property, PropertiesSource source, Object instance, Class<?> targetType) {
		if (property.isEntity()) {
			return readEntityProperty(property, instance, source);
		}

		if (!source.isPropertyPresent(property))
			return null;

		Object value = source.getPropertyValue(property);
		return new MutableObject<>(readValue(value, targetType));
	}
	
	public Object readValue(@Nullable Object value, Class<?> type) {
		if (null == value)
			return null;
		
		value = client.getSchemaDialect().convertFromDataBase(value, type);

		if (conversions.hasCustomReadTarget(value.getClass(), type)) {
			return conversionService.convert(value, type);
		} else if ((value instanceof String) && char[].class.equals(type)) {
			return ((String)value).toCharArray();
		} else {
			return getPotentiallyConvertedSimpleRead(value, type);
		}
	}

	/**
	 * Checks whether we have a custom conversion for the given simple object. Converts the given value if so, applies
	 * {@link Enum} handling or returns the value as is.
	 *
	 * @param value
	 * @param target must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Nullable
	protected Object getPotentiallyConvertedSimpleRead(@Nullable Object value, @Nullable Class<?> target) {
		if (value == null || target == null || ClassUtils.isAssignableValue(target, value))
			return value;

		if (conversions.hasCustomReadTarget(value.getClass(), target))
			return conversionService.convert(value, target);

		if (Enum.class.isAssignableFrom(target))
			return Enum.valueOf((Class<Enum>) target, value.toString());

		return conversionService.convert(value, target);
	}

	protected <T> MutableObject<T> readEntityProperty(RelationalPersistentProperty property, Object parentInstance, PropertiesSource source) {
		@SuppressWarnings("unchecked")
		RelationalPersistentEntity<T> entityType = (RelationalPersistentEntity<T>) client.getMappingContext().getRequiredPersistentEntity(property.getActualType());

		if (property.isAnnotationPresent(ForeignKey.class))
			return readForeignKeyEntity(property, parentInstance, entityType, source);

		// should we support embedded object ?
		throw new MappingException("Sub-entity without @ForeignKey is not supported: " + property.getName());
	}
	
	protected <T> MutableObject<T> readForeignKeyEntity(RelationalPersistentProperty property, Object parentInstance, RelationalPersistentEntity<T> entityType, PropertiesSource source) {
		if (!source.isPropertyPresent(property))
			return null;

		Object value = source.getPropertyValue(property);
		if (value == null)
			return new MutableObject<>(null); // foreign key is null
		T instance = getOrCreateInstance(entityType, source, value);
		EntityState state = EntityState.get(instance, client, entityType);
		if (!state.isLoaded()) {
			state.setPersistedField(instance, entityType.getRequiredIdProperty().getField(), value, true);
		}
		if (parentInstance != null)
			ModelUtils.setReverseLink(instance, parentInstance, property);
		if (!state.isLoaded())
			state.lazyLoaded();
		return new MutableObject<>(instance);
	}
	
	protected <T> T getOrCreateInstance(RelationalPersistentEntity<T> entityType, PropertiesSource source) {
		Object id;
		
		try {
			id = ModelUtils.getId(entityType, source);
		} catch (Exception e) {
			// not available
			id = null;
		}

		return getOrCreateInstance(entityType, source, id);
	}
	
	protected <T> T getOrCreateInstance(RelationalPersistentEntity<T> entityType, PropertiesSource source, Object id) {
		if (id != null) {
			T instance = cache.getById(entityType.getType(), id);
			if (instance != null)
				return instance;
		}
		
		PropertiesSourceParameterValueProvider parameterValueProvider = new PropertiesSourceParameterValueProvider(entityType, source);
		T instance = client.getMapper().createInstance(entityType, parameterValueProvider::getParameterValue);
		
		if (id != null)
			cache.setById(entityType.getType(), id, instance);
		
		return instance;
	}
	
	public class PropertiesSourceParameterValueProvider implements ParameterValueProvider<RelationalPersistentProperty> {

		private final RelationalPersistentEntity<?> entityType;
		private final PropertiesSource source;

		public PropertiesSourceParameterValueProvider(RelationalPersistentEntity<?> entityType, PropertiesSource source) {
			this.entityType = entityType;
			this.source = source;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.model.ParameterValueProvider#getParameterValue(org.springframework.data.mapping.PreferredConstructor.Parameter)
		 */
		@Override
		@Nullable
		public <T> T getParameterValue(Parameter<T, RelationalPersistentProperty> parameter) {
			String paramName = parameter.getName();
			Assert.notNull(paramName, "Parameter name must not be null");
			RelationalPersistentProperty property = entityType.getPersistentProperty(paramName);
			if (property == null)
				return null;
			Class<T> type = parameter.getType().getType();

			MutableObject<Object> value = readProperty(property, source, null, type);
			if (value == null)
				return null;
			Object v = value.getValue();

			if (type.isInstance(v)) {
				return type.cast(v);
			}
			return conversionService.convert(v, type);
		}
	}
	
}
