package com.myopty.order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The four optical values of a single eye (ISO 8594-1), expressed in dioptres.
 *
 * @param sphere   near/sight correction, {@code -30.00} to {@code +30.00}
 * @param cylinder astigmatism correction, {@code -10.00} to {@code +10.00}, usually zero or negative
 * @param axis     astigmatism orientation in whole degrees, {@code 0} to {@code 180};
 *                 only meaningful when {@code cylinder} is non-zero
 * @param add      near addition for presbyopic/progressive lenses, {@code 0.25} to {@code 4.00};
 *                 {@code null} when the customer has no reading addition
 */
public record EyePrescription(BigDecimal sphere, BigDecimal cylinder, Integer axis, BigDecimal add) {

	public static final BigDecimal MIN_SPHERE = new BigDecimal("-30.00");
	public static final BigDecimal MAX_SPHERE = new BigDecimal("30.00");
	public static final BigDecimal MIN_CYLINDER = new BigDecimal("-10.00");
	public static final BigDecimal MAX_CYLINDER = new BigDecimal("10.00");
	public static final BigDecimal NO_ADD = new BigDecimal("0.00");
	public static final BigDecimal MIN_ADD = new BigDecimal("0.25");
	public static final BigDecimal MAX_ADD = new BigDecimal("4.00");
	public static final int MIN_AXIS = 0;
	public static final int MAX_AXIS = 180;

	private static final BigDecimal QUARTER_STEP_HUNDREDTHS = new BigDecimal("25");

	/**
	 * Normalises a submitted optical value: trims, defaults blank input to
	 * {@code null} and drops insignificant decimals so that {@code "-2.50"} and
	 * {@code "-2.5"} compare equal.
	 */
	public static BigDecimal normalise(BigDecimal value) {
		if (value == null) {
			return null;
		}
		BigDecimal stripped = value.stripTrailingZeros();
		return stripped.scale() <= 2 ? stripped : value.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * Normalises a near addition. {@code 0.00} and blank both mean "no addition",
	 * so they collapse to {@code null} instead of being stored as a zero
	 * prescription value.
	 */
	public static BigDecimal normaliseAdd(BigDecimal value) {
		BigDecimal normalised = normalise(value);
		return normalised != null && normalised.signum() == 0 ? null : normalised;
	}

	public boolean hasCylinder() {
		return cylinder != null && cylinder.signum() != 0;
	}

	public boolean hasAdd() {
		return add != null && add.signum() > 0;
	}

	/**
	 * A prescription value must land on a 0.25 dioptre step; anything else is a
	 * typo (for example {@code -2.10}) rather than a real measurement. Shifting to
	 * hundredths and taking the remainder over 25 checks exactly that: a value is a
	 * quarter step when it is a whole number of quarter dioptres.
	 */
	public static boolean isQuarterStep(BigDecimal value) {
		if (value == null) {
			return true;
		}
		BigDecimal remainder = value.movePointRight(2).remainder(QUARTER_STEP_HUNDREDTHS);
		return remainder.signum() == 0;
	}

	public static boolean isWithin(BigDecimal value, BigDecimal min, BigDecimal max) {
		return value == null || (value.compareTo(min) >= 0 && value.compareTo(max) <= 0);
	}

}
