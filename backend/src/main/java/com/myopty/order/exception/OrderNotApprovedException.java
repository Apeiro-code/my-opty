package com.myopty.order.exception;

/**
 * Raised when a client tries to put a receive date on an order that has not been
 * approved. Mapped to HTTP 409.
 *
 * <p>The receive date is the shop's answer to "when will this be ready", and it
 * only exists once the shop has accepted the work. Quoting a date on an order that
 * is still awaiting review, or that was turned down, would be promising something
 * for work the shop has not taken on.
 */
public class OrderNotApprovedException extends RuntimeException {

	private final Long orderId;

	private final String currentStatus;

	public OrderNotApprovedException(Long orderId, String currentStatus) {
		super("Order " + orderId + " is " + currentStatus
			+ ", so it has no receive date until the shop approves it");
		this.orderId = orderId;
		this.currentStatus = currentStatus;
	}

	public Long getOrderId() {
		return this.orderId;
	}

	/**
	 * @return the status that blocks the date, so a client can tell an order still
	 *         awaiting review from one the shop rejected
	 */
	public String getCurrentStatus() {
		return this.currentStatus;
	}

}
