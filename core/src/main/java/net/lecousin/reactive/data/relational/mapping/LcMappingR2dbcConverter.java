package net.lecousin.reactive.data.relational.mapping;

import java.util.Collection;
import java.util.Collections;

import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.model.PropertiesSourceRow;

public class LcMappingR2dbcConverter extends MappingR2dbcConverter implements R2dbcConverter {

	
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
	
	public LcReactiveDataRelationalClient getLcClient() {
		return client;
	}

	@Override
	public <R> R read(Class<R> type, Row row, @Nullable RowMetadata metadata) {
		return new LcEntityReader(null, null, client).read(type, new PropertiesSourceRow(row, metadata));
	}
	
	@Override
	public Object readValue(@Nullable Object value, TypeInformation<?> type) {
		return new LcEntityReader(null, null, client).readValue(value, type);
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
		new LcEntityWriter(this).write(source, sink);
	}

	/**
	 * Returns given object as {@link Collection}. Will return the {@link Collection} as is if the source is a
	 * {@link Collection} already, will convert an array into a {@link Collection} or simply create a single element
	 * collection for everything else.
	 *
	 * @param source
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> Collection<T> asCollection(Object source) {

		if (source instanceof Collection) {
			return (Collection<T>) source;
		}

		return source.getClass().isArray() ? CollectionUtils.arrayToList(source) : Collections.singleton((T) source);
	}

}
