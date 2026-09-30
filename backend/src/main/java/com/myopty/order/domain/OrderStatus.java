package com.myopty.order.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Where an order has got to in the shop's workflow, and which states it may move
 * to from here.
 *
 * <p>{@link #PENDING_REVIEW} is the only state written while the order module
 * accepts customer orders; a customer chooses the order type but never the
 * workflow state. The client then reviews the order and moves it along. The enum
 * carries the full vocabulary so the column, the check constraint and the API
 * contract stay in step, and so a status is never invented ad hoc as a string in
 * a later story.
 *
 * <p>The legal moves live here rather than in the service so that the review
 * decisions and the production steps are described by one thing. Both consult
 * {@link #canAdvanceTo}, and {@code OrderStatusTest} asserts the whole matrix, so
 * a state added later without deciding what follows it fails the build.
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
	REJECTED;

	/**
	 * Whether an order in this state may move to {@code next}.
	 *
	 * <p>Forward only. Nothing ever goes back a step, because a status that could
	 * be undone would make the history of an order ambiguous: a customer told an
	 * order is ready should not be able to find it back in the lab. Steps can be
	 * skipped, though. A client handing over a frame that was already in stock has
	 * not had it processed and does not need to pretend otherwise, and refusing
	 * that would only push them into recording a state that never happened.
	 *
	 * <p>{@link #DISPATCHED} and {@link #REJECTED} answer false to everything,
	 * which is what makes them terminal.
	 *
	 * @param next the state being moved to, or null for a state read back from a row
	 *             written before this column existed
	 * @return true if the move is one the shop is allowed to make
	 */
	public boolean canAdvanceTo(OrderStatus next) {
		return next != null && successorsOf(this).contains(next);
	}

	/**
	 * @return every state this one may move to, so the rule is readable in one place
	 *         and a caller that needs to show the options does not re-derive them
	 */
	public Set<OrderStatus> successors() {
		return successorsOf(this);
	}

	private static Set<OrderStatus> successorsOf(OrderStatus from) {
		EnumSet<OrderStatus> successors = switch (from) {
			// The review decision. Nothing is made until a client accepts the work.
			case PENDING_REVIEW -> EnumSet.of(APPROVED, REJECTED);
			// From here the order is in production and moves through the workshop.
			case APPROVED -> EnumSet.of(PROCESSING, READY, DISPATCHED);
			case PROCESSING -> EnumSet.of(READY, DISPATCHED);
			case READY -> EnumSet.of(DISPATCHED);
			// Terminal: the order is either with the customer or was turned down.
			case DISPATCHED, REJECTED -> EnumSet.noneOf(OrderStatus.class);
		};
		return Collections.unmodifiableSet(successors);
	}

}
