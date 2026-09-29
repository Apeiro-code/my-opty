package com.myopty.order.exception;

/**
 * Raised when the uploaded document is missing, empty, too large, or not a
 * supported image/PDF. Mapped to HTTP 400.
 */
public class InvalidPrescriptionDocumentException extends RuntimeException {

	public InvalidPrescriptionDocumentException(String message) {
		super(message);
	}

	public InvalidPrescriptionDocumentException(String message, Throwable cause) {
		super(message, cause);
	}

}
