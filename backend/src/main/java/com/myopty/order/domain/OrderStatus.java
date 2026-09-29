package com.myopty.order.domain;

/**
 * Where an order has got to in the shop's workflow.
 *
 * <p>{@link #PENDING_REVIEW} is the only state written while the order module
 * accepts customer orders; a customer chooses the order type but never the
 * workflow state. The client review story then moves an order to
 * {@link #APPROVED} or {@link #REJECTED}, and the later production steps belong
 * to a future story. The enum carries the full vocabulary so the column, the
 * check constraint and the API contract stay in step, and so a status is never
 * invented ad hoc as a string in a later story.
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
	DISPATCHED,

	/**
	 * Turned down by the shop, with the reason in {@code rejection_reason}.
	 *
	 * <p>Terminal, and deliberately not part of the happy path above: a client
	 * that rejects an order by mistake has no way back to {@link #PENDING_REVIEW}.
	 * A one-way decision keeps the status history unambiguous, and reversing a
	 * rejection is a separate story rather than a flag on this one.
	 *
	 * <p>Independent of the prescription's own status. A client may reject an
	 * order for a reason that leaves the prescription perfectly valid, such as
	 * the frame being out of stock, so rejecting an order never rejects the
	 * prescription it was built from.
	 */
	REJECTED

}
