package com.myopty.order.dto;

import com.myopty.order.domain.OrderType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Body of {@code POST /api/orders}.
 *
 * <p>{@code orderType} is the type the customer selected; it is the field that
 * routes the order, so it is required rather than defaulted. {@code customerId} is
 * optional because the order inherits the owner of the prescription it is built
 * from; it only has to be sent when the client already knows it, and when it is
 * sent it has to agree.
 *
 * <p>{@code frameId} and {@code lensId} are optional and unvalidated beyond being
 * a positive id: the catalog tables that would own them do not exist yet, so
 * there is nothing to check them against.
 */
public record OrderCreateRequest(

		Long customerId,

		@NotNull(message = "is required") @Positive(message = "must be a positive id") Long prescriptionId,

		@NotNull(message = "is required") OrderType orderType,

		@Positive(message = "must be a positive id") Long frameId,

		@Positive(message = "must be a positive id") Long lensId) {

}
