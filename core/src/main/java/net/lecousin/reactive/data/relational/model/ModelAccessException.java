package net.lecousin.reactive.data.relational.model;

public class ModelAccessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ModelAccessException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ModelAccessException(String message) {
		super(message);
	}

}
