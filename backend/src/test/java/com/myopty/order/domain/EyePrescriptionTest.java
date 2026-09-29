package com.myopty.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The value object behind the prescription rules: decimal normalisation, the
 * quarter-dioptre step and the clinical ranges.
 */
class EyePrescriptionTest {

	@ParameterizedTest
	@ValueSource(strings = { "-2.50", "-2.5", "-2.500", "0.00", "1.25" })
	void normalisesEquivalentDecimals(String value) {
		BigDecimal normalised = EyePrescription.normalise(new BigDecimal(value));

		assertThat(normalised.stripTrailingZeros()).isEqualByComparingTo(new BigDecimal(value).stripTrailingZeros());
		assertThat(normalised.scale()).isLessThanOrEqualTo(2);
	}

	@Test
	void keepsAbsentValuesAbsent() {
		assertThat(EyePrescription.normalise(null)).isNull();
		assertThat(EyePrescription.normaliseAdd(null)).isNull();
	}

	@Test
	void treatsAZeroAdditionAsNoAddition() {
		assertThat(EyePrescription.normaliseAdd(new BigDecimal("0.00"))).isNull();
		assertThat(EyePrescription.normaliseAdd(new BigDecimal("-0.00"))).isNull();
		assertThat(EyePrescription.normaliseAdd(new BigDecimal("2.00"))).isEqualByComparingTo("2.00");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0.00", "0.25", "-0.25", "-2.75", "3.50" })
	void acceptsQuarterDioptreSteps(String value) {
		assertThat(EyePrescription.isQuarterStep(new BigDecimal(value))).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "0.10", "-2.13", "1.05" })
	void rejectsValuesOffTheQuarterStep(String value) {
		assertThat(EyePrescription.isQuarterStep(new BigDecimal(value))).isFalse();
	}

	@ParameterizedTest
	@CsvSource({ "-30.00, true", "30.00, true", "30.01, false", "-30.01, false" })
	void boundsTheSphere(String value, boolean withinRange) {
		assertThat(EyePrescription.isWithin(new BigDecimal(value), EyePrescription.MIN_SPHERE, EyePrescription.MAX_SPHERE))
			.isEqualTo(withinRange);
	}

	@ParameterizedTest
	@CsvSource({ "-10.00, true", "10.00, true", "10.25, false" })
	void boundsTheCylinder(String value, boolean withinRange) {
		assertThat(
				EyePrescription.isWithin(new BigDecimal(value), EyePrescription.MIN_CYLINDER, EyePrescription.MAX_CYLINDER))
			.isEqualTo(withinRange);
	}

	@ParameterizedTest
	@CsvSource({ "0.00, true", "4.00, true", "4.25, false" })
	void boundsTheAddition(String value, boolean withinRange) {
		assertThat(EyePrescription.isWithin(new BigDecimal(value), EyePrescription.NO_ADD, EyePrescription.MAX_ADD))
			.isEqualTo(withinRange);
	}

	@Test
	void treatsAnAbsentValueAsWithinEveryRange() {
		assertThat(EyePrescription.isWithin(null, EyePrescription.MIN_SPHERE, EyePrescription.MAX_SPHERE)).isTrue();
		assertThat(EyePrescription.isQuarterStep(null)).isTrue();
	}

	@Test
	void distinguishesAnAbsentCylinderFromAZeroOne() {
		assertThat(eye(null, null).hasCylinder()).isFalse();
		assertThat(eye("-1.00", "0.00").hasCylinder()).isFalse();
		assertThat(eye("-1.00", "-0.50").hasCylinder()).isTrue();
	}

	@Test
	void reportsAnAdditionOnlyWhenItIsPositive() {
		assertThat(eye("-1.00", null).hasAdd()).isFalse();
		assertThat(new EyePrescription(new BigDecimal("-1.00"), null, null, new BigDecimal("1.75")).hasAdd()).isTrue();
	}

	private static EyePrescription eye(String sphere, String cylinder) {
		return new EyePrescription(sphere == null ? null : new BigDecimal(sphere),
				cylinder == null ? null : new BigDecimal(cylinder), null, null);
	}

}
