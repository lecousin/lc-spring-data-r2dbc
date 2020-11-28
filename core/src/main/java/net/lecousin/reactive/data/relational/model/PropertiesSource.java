package net.lecousin.reactive.data.relational.model;

import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

public interface PropertiesSource {

	Object getSource();
	
	Object getPropertyValue(RelationalPersistentProperty property);
	
	boolean isPropertyPresent(RelationalPersistentProperty property);

}
