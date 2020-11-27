package net.lecousin.reactive.data.relational.model;

import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import io.r2dbc.spi.Row;

public interface PropertiesSource {

	Object getSource();
	
	Object getPropertyValue(RelationalPersistentProperty property);
	
	boolean isPropertyPresent(RelationalPersistentProperty property);
	
	Row asRow();

}
