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

import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.model.PropertiesSourceRow;

/**
 * 
 * @author Guillaume Le Cousin
 *
 */
public class LcMappingR2dbcConverter extends MappingR2dbcConverter implements R2dbcConverter {

	private LcReactiveDataRelationalClient client;
	
	public LcMappingR2dbcConverter(
		MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> context,
		CustomConversions conversions
	) {
		super(context, conversions);
	}
	
	public LcReactiveDataRelationalClient getLcClient() {
		return client;
	}
	
	public void setLcClient(LcReactiveDataRelationalClient client) {
		this.client = client;
	}

	@Override
	public <R> R read(Class<R> type, Row row, @Nullable RowMetadata metadata) {
		return new LcEntityReader(null, null, client).read(type, new PropertiesSourceRow(row, metadata));
	}
	
	@Override
	public Object readValue(@Nullable Object value, TypeInformation<?> type) {
		return new LcEntityReader(null, null, client).readValue(value, type.getType());
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

}
