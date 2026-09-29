package com.myopty.order.exception;

/**
 * Raised when a client tries to review a prescription that has already been
 * decided. Mapped to HTTP 409.
 *
 * <p>A conflict rather than a 400 because the request was well formed: the
 * prescription exists and the transition is a real one, it has simply already
 * happened. Reviewing is one-way, so there is no request that would succeed
 * against a prescription in this state.
 */
public class PrescriptionNotReviewableException extends RuntimeException {

	private final Long prescriptionId;

	private final String currentStatus;

	public PrescriptionNotReviewableException(Long prescriptionId, String currentStatus) {
		super("Prescription " + prescriptionId + " is " + currentStatus
			+ " and has already been reviewed, so it cannot be reviewed again");
		this.prescriptionId = prescriptionId;
		this.currentStatus = currentStatus;
	}

	public Long getPrescriptionId() {
		return this.prescriptionId;
	}

	/**
	 * @return the status that blocks the review, so a client can tell "already
	 *         verified" from "already rejected" without parsing the message.
	 */
	public String getCurrentStatus() {
		return this.currentStatus;
	}

}
