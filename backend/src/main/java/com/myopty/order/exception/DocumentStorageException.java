package com.myopty.order.exception;

/**
 * Raised when object storage cannot accept or return a document. Mapped to
 * HTTP 502 so the shop can tell an infrastructure fault apart from a customer
 * input error, and retry without asking the customer to re-enter anything.
 */
public class DocumentStorageException extends RuntimeException {

	public DocumentStorageException(String message, Throwable cause) {
		super(message, cause);
	}

	public DocumentStorageException(String message) {
		super(message);
	}

}
