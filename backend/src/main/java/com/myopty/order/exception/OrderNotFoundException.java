package com.myopty.order.exception;

/**
 * Raised when an order id does not exist. Mapped to HTTP 404.
 */
public class OrderNotFoundException extends RuntimeException {

	private final Long orderId;

	public OrderNotFoundException(Long orderId) {
		super("Order " + orderId + " was not found");
		this.orderId = orderId;
	}

	/**
	 * Used for the reverse lookup, where the id the customer has is the
	 * prescription's and the order is the thing that is missing.
	 */
	public OrderNotFoundException(String message) {
		super(message);
		this.orderId = null;
	}

	public Long getOrderId() {
		return this.orderId;
	}

}
