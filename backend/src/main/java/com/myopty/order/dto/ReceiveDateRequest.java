package com.myopty.order.dto;

import java.time.LocalDate;

/**
 * Body of {@code PUT /api/orders/{id}/receive-date}.
 *
 * <p>The field is nullable on purpose: {@code null} is how the shop withdraws an
 * estimate it can no longer stand behind, which is a different act from correcting
 * it. A body with no {@code receiveDate} at all is the same request, so a client
 * that forgets the field clears the date rather than failing validation. That is
 * the safer of the two mistakes here, since a client trying to set a date always
 * sends one, whereas one clearing it by accident is the case worth catching.
 *
 * <p>There is no {@code @NotNull} and no {@code @FutureOrPresent}: both rules are
 * enforced in the service, where the shop's current date is known, so the same
 * check applies to a date arriving from anywhere rather than only from this body.
 */
public record ReceiveDateRequest(LocalDate receiveDate) {

}
