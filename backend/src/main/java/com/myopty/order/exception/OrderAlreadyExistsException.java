package com.myopty.order.exception;

/**
 * Raised when a prescription already has an order, so a second one would break the
 * one-to-one link the module EER diagram requires. Mapped to HTTP 409, not 400,
 * because the request was well formed: the conflict is with an order that already
 * exists, and retrying it unchanged will never succeed.
 */
public class OrderAlreadyExistsException extends RuntimeException {

	private final Long prescriptionId;

	public OrderAlreadyExistsException(Long prescriptionId) {
		super("Prescription " + prescriptionId + " has already been ordered");
		this.prescriptionId = prescriptionId;
	}

	public Long getPrescriptionId() {
		return this.prescriptionId;
	}

}
