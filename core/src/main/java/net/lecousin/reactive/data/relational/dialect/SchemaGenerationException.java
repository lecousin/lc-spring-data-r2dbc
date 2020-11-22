package net.lecousin.reactive.data.relational.dialect;

import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * Runtime exception raised if the schema cannot be generated.
 */
public class SchemaGenerationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SchemaGenerationException(String message) {
		super(message);
	}

	public SchemaGenerationException(RelationalPersistentProperty property, String message) {
		super("Property " + property.getName() + " in entity " + property.getOwner().getName() + ": " + message);
	}
	
}
