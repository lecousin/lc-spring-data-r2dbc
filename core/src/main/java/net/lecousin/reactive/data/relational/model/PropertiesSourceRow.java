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
package net.lecousin.reactive.data.relational.model;

import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.lang.Nullable;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

/**
 * Implementation of PropertiesSource from a row returned by the database.
 * 
 * @author Guillaume Le Cousin
 *
 */
public class PropertiesSourceRow implements PropertiesSource {
	
	private Row row;
	private RowMetadata metadata;

	public PropertiesSourceRow(Row row, @Nullable RowMetadata metadata) {
		this.row = row;
		this.metadata = metadata;
	}
	
	@Override
	public Object getSource() {
		return row;
	}
	
	@Override
	public boolean isPropertyPresent(RelationalPersistentProperty property) {
		return metadata == null || metadata.getColumnNames().contains(property.getColumnName().toString());
	}
	
	@Override
	public Object getPropertyValue(RelationalPersistentProperty property) {
		return row.get(property.getColumnName().toString());
	}
	
}
