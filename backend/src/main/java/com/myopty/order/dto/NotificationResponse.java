package com.myopty.order.dto;

import java.time.Instant;

/**
 * One notification as a customer or client reads it.
 *
 * <p>Carries {@code fromStatus} and {@code toStatus} alongside the message because
 * the message is written for a person, and a client rendering a timeline wants the
 * states rather than having to parse English out of them.
 */
public record NotificationResponse(Long id, Long orderId, Long customerId, String fromStatus, String toStatus,
		String message, Instant createdAt) {
}
