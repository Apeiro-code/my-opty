package com.myopty.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of the reject actions on both the prescription and the order.
 *
 * <p>One record for both because the two payloads are identical, and the module
 * already shares a single {@link EyeRequest} between a prescription's two eyes
 * rather than declaring the same shape twice.
 *
 * <p>The reason is required. A rejection with no explanation cannot be acted on
 * by whoever is told about it, and it would leave the client having to reopen the
 * order to find out what went wrong. 500 is the length of
 * {@code progressive_order.rejection_reason} in
 * {@code V9__order_add_order_rejection.sql}, and matches
 * {@code prescription.rejection_reason} from V3 so both tables take the same
 * amount of text.
 */
public record RejectionRequest(

		@NotBlank(message = "is required") @Size(max = 500, message = "must be at most 500 characters") String reason) {

}
