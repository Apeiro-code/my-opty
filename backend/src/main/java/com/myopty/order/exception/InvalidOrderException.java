package com.myopty.order.exception;

import java.util.Map;

/**
 * Raised when an order breaks a rule that spans the order and the prescription it
 * is built from, which bean validation cannot express because it needs both rows
 * in hand. Mapped to HTTP 400 with the offending field paths.
 */
public class InvalidOrderException extends RuntimeException {

	private final Map<String, String> fieldErrors;

	public InvalidOrderException(String message, Map<String, String> fieldErrors) {
		super(message);
		this.fieldErrors = Map.copyOf(fieldErrors);
	}

	public Map<String, String> getFieldErrors() {
		return this.fieldErrors;
	}

}
