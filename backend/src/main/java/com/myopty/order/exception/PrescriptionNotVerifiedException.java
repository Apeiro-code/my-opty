package com.myopty.order.exception;

/**
 * Raised when a client tries to approve an order whose prescription has not been
 * verified. Mapped to HTTP 409.
 *
 * <p>This is the rule that makes the prescription review story load-bearing: an
 * order cannot be approved for production until the prescription it was built
 * from has been verified, so unverified or rejected prescriptions can never
 * reach the lab. Both the order and the prescription are valid on their own, so
 * the conflict belongs to the pair rather than to either row.
 */
public class PrescriptionNotVerifiedException extends RuntimeException {

	private final Long prescriptionId;

	private final String currentStatus;

	public PrescriptionNotVerifiedException(Long prescriptionId, String currentStatus) {
		super("Order cannot be approved: prescription " + prescriptionId + " is " + currentStatus
			+ ", not VERIFIED");
		this.prescriptionId = prescriptionId;
		this.currentStatus = currentStatus;
	}

	public Long getPrescriptionId() {
		return this.prescriptionId;
	}

	/**
	 * @return the prescription's actual status, so a client can point the reviewer
	 *         at the prescription that still needs work.
	 */
	public String getCurrentStatus() {
		return this.currentStatus;
	}

}
