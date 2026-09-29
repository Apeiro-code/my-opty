package com.myopty.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A prescription as returned to the customer and the client.
 *
 * @param document metadata of the uploaded scan or photo; {@code url} is the
 *                 download path, the object storage key is never exposed
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PrescriptionResponse(Long id, Long customerId, String status, boolean progressive, LocalDate issuedDate,
		EyeResponse rightEye, EyeResponse leftEye, String notes, String rejectionReason, DocumentResponse document,
		Instant createdAt, Instant updatedAt) {

	public record EyeResponse(BigDecimal sphere, BigDecimal cylinder, Integer axis, BigDecimal addPower) {
	}

	public record DocumentResponse(String filename, String contentType, Long sizeBytes, Instant uploadedAt, String url) {
	}

}
