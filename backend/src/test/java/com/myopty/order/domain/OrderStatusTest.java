package com.myopty.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The workflow an order is allowed to follow.
 *
 * <p>The whole matrix is asserted rather than a few examples, because this is the
 * rule the approve, reject and three production endpoints all delegate to. A state
 * added later without deciding what may follow it fails here, instead of being
 * accepted by the column and then found at runtime.
 */
class OrderStatusTest {

	/**
	 * The complete set of legal moves. Written out in full so that adding a status
	 * forces a decision about it here.
	 *
	 * <p>Steps may be skipped, so {@code APPROVED} can reach every later state
	 * directly: a client handing over a frame that was already in stock has not had
	 * it processed, and recording a state that never happened would be a worse lie
	 * than the skip.
	 */
	private static final Set<List<OrderStatus>> LEGAL_MOVES = Set.of(List.of(OrderStatus.PENDING_REVIEW, OrderStatus.APPROVED),
			List.of(OrderStatus.PENDING_REVIEW, OrderStatus.REJECTED), List.of(OrderStatus.APPROVED, OrderStatus.PROCESSING),
			List.of(OrderStatus.APPROVED, OrderStatus.READY), List.of(OrderStatus.APPROVED, OrderStatus.DISPATCHED),
			List.of(OrderStatus.PROCESSING, OrderStatus.READY), List.of(OrderStatus.PROCESSING, OrderStatus.DISPATCHED),
			List.of(OrderStatus.READY, OrderStatus.DISPATCHED));

	@Test
	void allowsExactlyTheMovesTheWorkflowDescribes() {
		for (OrderStatus from : OrderStatus.values()) {
			for (OrderStatus to : OrderStatus.values()) {
				boolean shouldBeAllowed = LEGAL_MOVES.contains(List.of(from, to));

				assertThat(from.canAdvanceTo(to)).as("%s -> %s", from, to).isEqualTo(shouldBeAllowed);
			}
		}
	}

	@Test
	void allowsEveryMoveInTheWorkflow() {
		LEGAL_MOVES.forEach(move -> assertThat(move.get(0).canAdvanceTo(move.get(1)))
			.as("%s -> %s", move.get(0), move.get(1))
			.isTrue());
	}

	/**
	 * Nothing goes back a step. A customer told an order is ready should not be able
	 * to find it back in the lab, so a status that could be undone would make the
	 * history of an order ambiguous.
	 */
	@Test
	void neverMovesBackwards() {
		for (OrderStatus from : OrderStatus.values()) {
			for (OrderStatus to : OrderStatus.values()) {
				if (from.ordinal() > to.ordinal()) {
					assertThat(from.canAdvanceTo(to)).as("%s -> %s", from, to).isFalse();
				}
			}
		}
	}

	/**
	 * A dispatched order is with the customer and a rejected one was never started,
	 * so neither has anywhere left to go. That is what makes them terminal.
	 */
	@Test
	void treatsDispatchedAndRejectedAsTerminal() {
		for (OrderStatus terminal : List.of(OrderStatus.DISPATCHED, OrderStatus.REJECTED)) {
			for (OrderStatus to : OrderStatus.values()) {
				assertThat(terminal.canAdvanceTo(to)).as("%s -> %s", terminal, to).isFalse();
			}
			assertThat(terminal.successors()).as("successors of %s", terminal).isEmpty();
		}
	}

	/**
	 * Nothing reaches production before the shop has accepted the work, which is the
	 * whole point of requiring the review decision first.
	 */
	@Test
	void neverEntersProductionWithoutBeingApproved() {
		for (OrderStatus to : List.of(OrderStatus.PROCESSING, OrderStatus.READY, OrderStatus.DISPATCHED)) {
			assertThat(OrderStatus.PENDING_REVIEW.canAdvanceTo(to)).isFalse();
		}
	}

	/**
	 * A status read back from a row written before this column existed can be null,
	 * and it has to be answered rather than throwing on a lookup a message quotes.
	 */
	@Test
	void refusesANullTarget() {
		assertThat(OrderStatus.APPROVED.canAdvanceTo(null)).isFalse();
	}

	@Test
	void reportsTheStatesAnOrderCanReach() {
		assertThat(OrderStatus.PENDING_REVIEW.successors())
			.isEqualTo(EnumSet.of(OrderStatus.APPROVED, OrderStatus.REJECTED));
		assertThat(OrderStatus.READY.successors()).containsExactly(OrderStatus.DISPATCHED);
	}

	/**
	 * The review decision is one decision with two outcomes, and the service relies
	 * on that: it asks whether an order can still reach {@code APPROVED} and then
	 * treats {@code REJECTED} the same way, since both are only reachable from
	 * {@code PENDING_REVIEW}. That is a fair assumption today, but a status added
	 * later that could be approved without being rejected would make {@code reject}
	 * refuse a decision the shop could actually make. Pinning the two together here
	 * means that shows up as a test failure rather than as a rejected order.
	 */
	@Test
	void allowsTheReviewDecisionFromTheSameStatesForBothOutcomes() {
		for (OrderStatus from : OrderStatus.values()) {
			assertThat(from.canAdvanceTo(OrderStatus.REJECTED))
				.as("reachability of REJECTED from %s must match APPROVED", from)
				.isEqualTo(from.canAdvanceTo(OrderStatus.APPROVED));
		}
	}

	/**
	 * The set is what a caller would use to show the available steps, so handing back
	 * the enum's own mutable set would let a caller change the workflow by accident.
	 */
	@Test
	void willNotLetACallerChangeTheWorkflowThroughTheReturnedSet() {
		Set<OrderStatus> successors = OrderStatus.APPROVED.successors();

		assertThat(successors).isUnmodifiable();
	}

}
