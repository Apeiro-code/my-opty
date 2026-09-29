package com.myopty.order.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * An order as the client sees it.
 *
 * <p>The enums are carried as their names rather than as JSON objects, so the
 * payload reads the same as the {@code status} of a prescription and matches the
 * values the migration's check constraints allow. {@code receiveDate} is omitted
 * until the shop has quoted one, because a guess is worse than an absence.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponse(Long id, Long customerId, Long prescriptionId, Long frameId, Long lensId, String orderType,
		String status, LocalDate receiveDate, Instant createdAt, Instant updatedAt) {

}
