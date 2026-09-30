package com.myopty.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The declared limits of the shared rejection body.
 *
 * <p>The same rules are enforced in both services as well, because a reason
 * arriving from anywhere other than these endpoints still has to fit the column.
 * That duplication is deliberate and is what these tests guard.
 */
class RejectionRequestTest {

	/**
	 * Length of {@code progressive_order.rejection_reason} from
	 * {@code V9__order_add_order_rejection.sql}, which matches
	 * {@code prescription.rejection_reason} from V3.
	 */
	private static final int MAX_REASON = 500;

	private static ValidatorFactory validatorFactory;

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		RejectionRequestTest.validatorFactory = Validation.buildDefaultValidatorFactory();
		RejectionRequestTest.validator = RejectionRequestTest.validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		RejectionRequestTest.validatorFactory.close();
	}

	/**
	 * A rejection with no explanation is not something the person told about it can
	 * act on, so the reason is required rather than optional.
	 */
	@Test
	void requiresAReason() {
		assertThat(violationsOf(null)).containsExactly("reason: is required");
	}

	@Test
	void refusesAReasonThatIsOnlyWhitespace() {
		assertThat(violationsOf("   ")).containsExactly("reason: is required");
	}

	/**
	 * The limit is the column's, so a longer reason is refused here rather than
	 * being truncated on the way into the database.
	 */
	@Test
	void refusesAReasonLongerThanTheColumn() {
		assertThat(violationsOf("x".repeat(MAX_REASON + 1)))
			.containsExactly("reason: must be at most 500 characters");
	}

	@Test
	void acceptsAReasonExactlyAtTheLimit() {
		assertThat(violationsOf("x".repeat(MAX_REASON))).isEmpty();
	}

	@Test
	void acceptsAnOrdinaryReason() {
		assertThat(violationsOf("left eye axis is missing")).isEmpty();
	}

	/**
	 * Property and message together, because a client renders the message against
	 * the field and "is required" is meaningless without knowing which field.
	 */
	private static Set<String> violationsOf(String reason) {
		return validator.validate(new RejectionRequest(reason))
			.stream()
			.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
			.collect(Collectors.toSet());
	}

}
