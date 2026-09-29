package com.myopty.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The declared limits of the prescription part, checked at the exact boundaries
 * so a customer can still enter the strongest lens we accept and no stronger.
 * The cross-field optical rules live in the service layer and are tested there.
 */
class PrescriptionCreateRequestTest {

	private static ValidatorFactory validatorFactory;

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		PrescriptionCreateRequestTest.validatorFactory = Validation.buildDefaultValidatorFactory();
		PrescriptionCreateRequestTest.validator = PrescriptionCreateRequestTest.validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		PrescriptionCreateRequestTest.validatorFactory.close();
	}

	@Test
	void acceptsACompletePrescription() {
		assertThat(invalidFieldsOf(request("-2.00", "-0.75", 180, "2.00", "-2.25", "-0.50", 90, "2.00"))).isEmpty();
	}

	@Test
	void acceptsAProgressiveLensWithoutAnyAddition() {
		PrescriptionCreateRequest request = new PrescriptionCreateRequest(7L, Boolean.FALSE, LocalDate.of(2026, 3, 14),
				new EyeRequest(new BigDecimal("-2.00"), null, null, null),
				new EyeRequest(new BigDecimal("-2.00"), null, null, null), null);

		assertThat(invalidFieldsOf(request)).isEmpty();
	}

	/**
	 * {@code progressive} is nullable on purpose: a client that omits it means
	 * single-vision and must not be told it is invalid.
	 */
	@Test
	void acceptsAnOmittedProgressiveFlag() {
		PrescriptionCreateRequest request = new PrescriptionCreateRequest(null, null, null,
				new EyeRequest(new BigDecimal("-2.00"), null, null, null),
				new EyeRequest(new BigDecimal("-2.00"), null, null, null), null);

		assertThat(invalidFieldsOf(request)).isEmpty();
	}

	@Test
	void acceptsTheExtremeValuesOfEveryRange() {
		PrescriptionCreateRequest request = request("-30.00", "-10.00", 0, "4.00", "30.00", "10.00", 180, "0.00");

		assertThat(invalidFieldsOf(request)).isEmpty();
	}

	@Test
	void rejectsValuesJustOutsideTheRanges() {
		assertThat(invalidFieldsOf(request("-30.01", "-10.01", -1, "4.01", "30.01", "10.01", 181, "-0.25")))
			.contains("rightEye.sphere", "rightEye.cylinder", "rightEye.axis", "rightEye.addPower", "leftEye.sphere",
					"leftEye.cylinder", "leftEye.axis", "leftEye.addPower");
	}

	@Test
	void rejectsAThirdDecimalPlace() {
		assertThat(invalidFieldsOf(request("-2.005", null, null, null, null, null, null, null)))
			.contains("rightEye.sphere");
	}

	@Test
	void requiresASphereForEachEye() {
		PrescriptionCreateRequest request = new PrescriptionCreateRequest(null, null, null,
				new EyeRequest(null, null, null, null), new EyeRequest(null, null, null, null), null);

		assertThat(invalidFieldsOf(request)).contains("rightEye.sphere", "leftEye.sphere");
	}

	@Test
	void requiresBothEyes() {
		PrescriptionCreateRequest request = new PrescriptionCreateRequest(null, null, null,
				new EyeRequest(new BigDecimal("-2.00"), null, null, null), null, null);

		assertThat(invalidFieldsOf(request)).contains("leftEye");
	}

	@Test
	void rejectsAnIssueDateInTheFuture() {
		PrescriptionCreateRequest request = new PrescriptionCreateRequest(null, null, LocalDate.now().plusDays(1),
				new EyeRequest(new BigDecimal("-2.00"), null, null, null),
				new EyeRequest(new BigDecimal("-2.00"), null, null, null), null);

		assertThat(invalidFieldsOf(request)).contains("issuedDate");
	}

	@Test
	void rejectsNotesLongerThanTheColumn() {
		PrescriptionCreateRequest request = new PrescriptionCreateRequest(null, null, null,
				new EyeRequest(new BigDecimal("-2.00"), null, null, null),
				new EyeRequest(new BigDecimal("-2.00"), null, null, null), "x".repeat(501));

		assertThat(invalidFieldsOf(request)).contains("notes");
	}

	private static Set<String> invalidFieldsOf(PrescriptionCreateRequest request) {
		return PrescriptionCreateRequestTest.validator.validate(request)
			.stream()
			.map(ConstraintViolation::getPropertyPath)
			.map(Object::toString)
			.collect(Collectors.toSet());
	}

	private static PrescriptionCreateRequest request(String sphRight, String cylRight, Integer axisRight,
			String addRight, String sphLeft, String cylLeft, Integer axisLeft, String addLeft) {
		return new PrescriptionCreateRequest(7L, true, LocalDate.of(2026, 3, 14),
				new EyeRequest(decimal(sphRight), decimal(cylRight), axisRight, decimal(addRight)),
				new EyeRequest(decimal(sphLeft), decimal(cylLeft), axisLeft, decimal(addLeft)), "usual frame");
	}

	private static BigDecimal decimal(String value) {
		return value == null ? null : new BigDecimal(value);
	}

}
