package com.myopty.order.exception;

import com.myopty.order.domain.OrderStatus;

/**
 * Raised when a client tries to move an order to a state it cannot reach from
 * where it is. Mapped to HTTP 409.
 *
 * <p>The counterpart of {@link OrderNotReviewableException} for the production
 * steps. That one covers the review decision, which is only ever available from
 * {@code PENDING_REVIEW}; this one covers everything after it, and covers two
 * distinct mistakes that would otherwise look the same: asking for a step in the
 * wrong order, and asking for a step at all on an order that has finished or was
 * turned down.
 */
public class OrderNotAdvancableException extends RuntimeException {

	private final Long orderId;

	private final String currentStatus;

	private final OrderStatus requestedStatus;

	public OrderNotAdvancableException(Long orderId, String currentStatus, OrderStatus requestedStatus) {
		super("Order " + orderId + " is " + currentStatus + ", so it cannot be moved to " + requestedStatus);
		this.orderId = orderId;
		this.currentStatus = currentStatus;
		this.requestedStatus = requestedStatus;
	}

	public Long getOrderId() {
		return this.orderId;
	}

	/**
	 * @return the status the order is actually in, so a client can tell an order
	 *         that is waiting to be processed from one the customer has collected
	 */
	public String getCurrentStatus() {
		return this.currentStatus;
	}

	/**
	 * @return the move that was refused, so the caller can say which one failed
	 *         rather than only that something did
	 */
	public OrderStatus getRequestedStatus() {
		return this.requestedStatus;
	}

}
