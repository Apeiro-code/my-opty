package com.myopty.order.exception;

/**
 * Raised when a prescription id does not exist. Mapped to HTTP 404.
 */
public class PrescriptionNotFoundException extends RuntimeException {

	private final Long prescriptionId;

	public PrescriptionNotFoundException(Long prescriptionId) {
		super("Prescription " + prescriptionId + " was not found");
		this.prescriptionId = prescriptionId;
	}

	public Long getPrescriptionId() {
		return this.prescriptionId;
	}

}
