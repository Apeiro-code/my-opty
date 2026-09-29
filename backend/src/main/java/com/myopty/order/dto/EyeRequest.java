package com.myopty.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Optical values for one eye, as typed by the customer.
 *
 * <p>Range annotations mirror the {@code prescription} table CHECK constraints;
 * the cross-field rules (axis only with cylinder, add power required for
 * progressive lenses, 0.25 dioptre steps) are enforced in the service layer.
 *
 * @param axis  whole degrees, {@code 0}-{@code 180}; only meaningful with a non-zero cylinder
 * @param addPower near addition in dioptres, {@code null} or {@code 0.00} when not needed
 */
public record EyeRequest(

		@NotNull(message = "is required") @DecimalMin(value = "-30.00", message = "must be between -30.00 and +30.00")
		@DecimalMax(value = "30.00", message = "must be between -30.00 and +30.00")
		@Digits(integer = 2, fraction = 2, message = "must have at most 2 decimal places") BigDecimal sphere,

		@DecimalMin(value = "-10.00", message = "must be between -10.00 and +10.00")
		@DecimalMax(value = "10.00", message = "must be between -10.00 and +10.00")
		@Digits(integer = 2, fraction = 2, message = "must have at most 2 decimal places") BigDecimal cylinder,

		@Min(value = 0, message = "must be between 0 and 180") @Max(value = 180, message = "must be between 0 and 180")
		Integer axis,

		@DecimalMin(value = "0.00", message = "must be between 0.00 and 4.00")
		@DecimalMax(value = "4.00", message = "must be between 0.00 and 4.00")
		@Digits(integer = 1, fraction = 2, message = "must have at most 2 decimal places") BigDecimal addPower) {

}
