package com.myopty.order.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/prescriptions}, sent as the {@code prescription}
 * part of a {@code multipart/form-data} request next to the {@code document} part.
 * <p>{@code progressive} is nullable so a client that omits it is not rejected;
 * an absent value means a single-vision lens.
 */
public record PrescriptionCreateRequest(

		Long customerId,

		Boolean progressive,

		@PastOrPresent(message = "must not be in the future") LocalDate issuedDate,

		@NotNull(message = "is required") @Valid EyeRequest rightEye,

		@NotNull(message = "is required") @Valid EyeRequest leftEye,

		@Size(max = 500, message = "must be at most 500 characters") String notes) {

}
