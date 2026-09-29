package com.myopty.order.exception;

/**
 * Raised when a client tries to approve or reject an order that is not awaiting
 * review. Mapped to HTTP 409.
 *
 * <p>The counterpart of {@link PrescriptionNotReviewableException} for orders: the
 * request is well formed but the order has already left {@code PENDING_REVIEW},
 * so no client decision applies to it.
 */
public class OrderNotReviewableException extends RuntimeException {

	private final Long orderId;

	private final String currentStatus;

	public OrderNotReviewableException(Long orderId, String currentStatus) {
		super("Order " + orderId + " is " + currentStatus
			+ ", so it is not awaiting a decision from the shop");
		this.orderId = orderId;
		this.currentStatus = currentStatus;
	}

	public Long getOrderId() {
		return this.orderId;
	}

	/**
	 * @return the status that blocks the decision, so a client can tell an order
	 *         that is already approved from one that has been rejected.
	 */
	public String getCurrentStatus() {
		return this.currentStatus;
	}

}
