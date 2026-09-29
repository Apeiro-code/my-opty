package com.myopty.order.domain;

/**
 * Verification state of a submitted prescription.
 *
 * <p>{@link #PENDING_REVIEW} is the only state a customer can create; the client
 * moves a prescription to {@link #VERIFIED} or {@link #REJECTED} once the uploaded
 * document has been checked against the typed values.
 */
public enum PrescriptionStatus {

	PENDING_REVIEW,
	VERIFIED,
	REJECTED

}
