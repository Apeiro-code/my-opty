package com.myopty.order.domain;

/**
 * Where an order has got to in the shop's workflow.
 *
 * <p>{@link #PENDING_REVIEW} is the only state written while the order module
 * accepts customer orders. Every later step is driven by the client, so the enum
 * carries the full vocabulary now to keep the column, the check constraint and
 * the API contract in step, and to stop a status ever being invented ad hoc as a
 * string in a later story.
 */
public enum OrderStatus {

	/**
	 * Placed by the customer and waiting for the shop to review the prescription
	 * and approve it.
	 */
	PENDING_REVIEW,

	/**
	 * The prescription has been verified and the order accepted for production.
	 */
	APPROVED,

	/**
	 * Being made in the lab.
	 */
	PROCESSING,

	/**
	 * Finished and waiting for the customer to collect.
	 */
	READY,

	/**
	 * Handed over or sent to the customer.
	 */
	DISPATCHED

}
