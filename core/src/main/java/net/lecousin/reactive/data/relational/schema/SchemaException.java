package net.lecousin.reactive.data.relational.schema;

/**
 * Runtime exception raised if the schema cannot be generated.
 */
public class SchemaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SchemaException(String message) {
		super(message);
	}
	
}
