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
package net.lecousin.reactive.data.relational.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.convert.CustomConversions.StoreConversions;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.r2dbc.core.DatabaseClient;

import io.r2dbc.spi.ConnectionFactory;
import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.mapping.LcMappingR2dbcConverter;
import net.lecousin.reactive.data.relational.mapping.LcR2dbcMappingContext;
import net.lecousin.reactive.data.relational.mapping.LcReactiveDataAccessStrategy;
import net.lecousin.reactive.data.relational.repository.LcR2dbcEntityTemplate;
import net.lecousin.reactive.data.relational.schema.dialect.RelationalDatabaseSchemaDialect;

/**
 * Helper class to initialize beans for a specific database connection.
 * 
 * @author Guillaume Le Cousin
 *
 */
public abstract class LcR2dbcEntityOperationsBuilder {

	protected LcR2dbcEntityTemplate buildEntityOperations(ConnectionFactory connectionFactory) {
		R2dbcDialect dialect = getDialect(connectionFactory);
		R2dbcCustomConversions customConversions = buildR2dbcCustomConversions(dialect);
		R2dbcMappingContext mappingContext = buildMappingContext();
		mappingContext.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());
		LcMappingR2dbcConverter converter = new LcMappingR2dbcConverter(mappingContext, customConversions);
		LcReactiveDataAccessStrategy dataAccessStrategy = new LcReactiveDataAccessStrategy(dialect, converter);
		DatabaseClient client = buildDatabaseClient(connectionFactory, dialect);
		LcReactiveDataRelationalClient lcClient = new LcReactiveDataRelationalClient(client, getLcDialect(dialect), dataAccessStrategy);
		return new LcR2dbcEntityTemplate(lcClient);
	}
	
	protected R2dbcDialect getDialect(ConnectionFactory connectionFactory) {
		return DialectResolver.getDialect(connectionFactory);
	}
	
	protected RelationalDatabaseSchemaDialect getLcDialect(R2dbcDialect dialect) {
		return RelationalDatabaseSchemaDialect.getDialect(dialect);
	}
	
	protected R2dbcMappingContext buildMappingContext() {
		return new LcR2dbcMappingContext(NamingStrategy.INSTANCE);
	}
	
	protected R2dbcCustomConversions buildR2dbcCustomConversions(R2dbcDialect dialect) {
		return new R2dbcCustomConversions(getStoreConversions(dialect), getCustomConverters());
	}

	protected List<Object> getCustomConverters() {
		return Collections.emptyList();
	}

	protected StoreConversions getStoreConversions(R2dbcDialect dialect) {
		List<Object> converters = new ArrayList<>(dialect.getConverters());
		converters.addAll(R2dbcCustomConversions.STORE_CONVERTERS);

		return StoreConversions.of(dialect.getSimpleTypeHolder(), converters);
	}
	
	protected DatabaseClient buildDatabaseClient(ConnectionFactory connectionFactory, R2dbcDialect dialect) {
		return DatabaseClient.builder() //
			.connectionFactory(connectionFactory)
			.bindMarkers(dialect.getBindMarkersFactory())
			.build();		
	}

}
