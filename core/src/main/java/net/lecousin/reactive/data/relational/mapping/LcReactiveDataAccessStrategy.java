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

import java.util.function.BiFunction;

import org.springframework.data.r2dbc.core.DefaultReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import net.lecousin.reactive.data.relational.model.PropertiesSourceRow;

/**
 * 
 * @author Guillaume Le Cousin
 *
 */
public class LcReactiveDataAccessStrategy extends DefaultReactiveDataAccessStrategy {

	protected R2dbcDialect dialect;
	
	public LcReactiveDataAccessStrategy(R2dbcDialect dialect, LcMappingR2dbcConverter converter) {
		super(dialect, converter);
		this.dialect = dialect;
	}
	
	@Override
	public <T> BiFunction<Row, RowMetadata, T> getRowMapper(Class<T> typeToRead) {
		LcEntityReader reader = new LcEntityReader(null, (LcMappingR2dbcConverter)getConverter());
		return (row, metadata) -> reader.read(typeToRead, new PropertiesSourceRow(row, metadata));
	}
	
	public R2dbcDialect getDialect() {
		return dialect;
	}

}
