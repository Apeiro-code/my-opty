package com.myopty.order.exception;

/**
 * Raised when a prescription exists but has no document attached. Mapped to
 * HTTP 404 rather than to a storage error: nothing about the object store is
 * broken, the document simply is not there to download.
 */
public class PrescriptionDocumentNotFoundException extends RuntimeException {

	private final Long prescriptionId;

	public PrescriptionDocumentNotFoundException(Long prescriptionId) {
		super("Prescription " + prescriptionId + " has no document attached");
		this.prescriptionId = prescriptionId;
	}

	public Long getPrescriptionId() {
		return this.prescriptionId;
	}

}
