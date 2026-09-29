package com.myopty.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import com.myopty.order.domain.OrderType;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The declared limits of the order body. The rules that need the prescription as
 * well as the request, such as whether the order type fits, live in the service
 * layer and are tested there.
 */
class OrderCreateRequestTest {

	private static ValidatorFactory validatorFactory;

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		OrderCreateRequestTest.validatorFactory = Validation.buildDefaultValidatorFactory();
		OrderCreateRequestTest.validator = OrderCreateRequestTest.validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		OrderCreateRequestTest.validatorFactory.close();
	}

	@Test
	void acceptsACompleteOrder() {
		assertThat(invalidFieldsOf(new OrderCreateRequest(7L, 12L, OrderType.PROGRESSIVE, 4L, 9L))).isEmpty();
	}

	/**
	 * The frame and lens are optional because the catalog tables that own them do
	 * not exist yet. A customer must not be stopped at the counter over a column
	 * the shop cannot validate anyway.
	 */
	@Test
	void acceptsAnOrderWithoutAFrameOrLens() {
		assertThat(invalidFieldsOf(new OrderCreateRequest(7L, 12L, OrderType.SINGLE_VISION, null, null))).isEmpty();
	}

	@Test
	void acceptsAnOrderWithoutACustomerBecauseItIsInheritedFromThePrescription() {
		assertThat(invalidFieldsOf(new OrderCreateRequest(null, 12L, OrderType.BIFOCAL, null, null))).isEmpty();
	}

	@Test
	void acceptsEveryOrderType() {
		for (OrderType orderType : OrderType.values()) {
			assertThat(invalidFieldsOf(new OrderCreateRequest(7L, 12L, orderType, null, null))).isEmpty();
		}
	}

	@Test
	void requiresAPrescription() {
		assertThat(invalidFieldsOf(new OrderCreateRequest(7L, null, OrderType.SINGLE_VISION, null, null)))
			.contains("prescriptionId");
	}

	@Test
	void requiresAnOrderType() {
		assertThat(invalidFieldsOf(new OrderCreateRequest(7L, 12L, null, null, null))).contains("orderType");
	}

	@Test
	void rejectsIdsThatCannotNameARow() {
		assertThat(invalidFieldsOf(new OrderCreateRequest(7L, 0L, OrderType.SINGLE_VISION, -1L, 0L)))
			.contains("prescriptionId", "frameId", "lensId");
	}

	private static Set<String> invalidFieldsOf(OrderCreateRequest request) {
		return OrderCreateRequestTest.validator.validate(request)
			.stream()
			.map(ConstraintViolation::getPropertyPath)
			.map(Object::toString)
			.collect(Collectors.toSet());
	}

}
