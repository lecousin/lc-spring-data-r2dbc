package net.lecousin.reactive.data.relational.model;

import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

public class ModelException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public ModelException(String message) {
		super(message);
	}
	
	public ModelException(RelationalPersistentEntity<?> entity, String message) {
		super("Entity " + entity.getName() + ": " + message);
	}
	
	public ModelException(RelationalPersistentProperty property, String message) {
		super("Property " + property.getName() + " in entity " + property.getOwner().getName() + ": " + message);
	}

	public ModelException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ModelException(RelationalPersistentEntity<?> entity, String message, Throwable cause) {
		super("Entity " + entity.getName() + ": " + message, cause);
	}
	
	public ModelException(RelationalPersistentProperty property, String message, Throwable cause) {
		super("Property " + property.getName() + " in entity " + property.getOwner().getName() + ": " + message, cause);
	}

}
