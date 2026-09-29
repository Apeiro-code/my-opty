package com.myopty.order.exception;

import java.util.Map;

/**
 * Raised when the submitted optical values break a prescription rule that bean
 * validation cannot express on a single field. Mapped to HTTP 400 with the
 * offending field paths.
 */
public class InvalidPrescriptionException extends RuntimeException {

	private final Map<String, String> fieldErrors;

	public InvalidPrescriptionException(String message, Map<String, String> fieldErrors) {
		super(message);
		this.fieldErrors = Map.copyOf(fieldErrors);
	}

	public Map<String, String> getFieldErrors() {
		return this.fieldErrors;
	}

}
