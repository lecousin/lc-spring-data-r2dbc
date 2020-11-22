package net.lecousin.reactive.data.relational.model;

public class InvalidEntityStateException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidEntityStateException(String message) {
		super(message);
	}

}
