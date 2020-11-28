package net.lecousin.reactive.data.relational.model;

import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.lang.Nullable;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

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
