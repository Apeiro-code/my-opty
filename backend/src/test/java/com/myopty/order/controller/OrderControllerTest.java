package com.myopty.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.myopty.order.domain.Order;
import com.myopty.order.domain.OrderStatus;
import com.myopty.order.domain.OrderType;
import com.myopty.order.exception.InvalidOrderException;
import com.myopty.order.exception.OrderAlreadyExistsException;
import com.myopty.order.exception.OrderExceptionHandler;
import com.myopty.order.exception.OrderNotFoundException;
import com.myopty.order.exception.PrescriptionNotFoundException;
import com.myopty.order.service.OrderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Endpoint behaviour for placing and retrieving an order: the JSON contract, the
 * documented response envelope and the documented error codes. In particular the
 * codes the order form on the client keys off, including the one for an order type
 * the API does not recognise.
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

	private static final String VALID_REQUEST = """
			{
			  "customerId": 7,
			  "prescriptionId": 12,
			  "orderType": "PROGRESSIVE",
			  "frameId": 4,
			  "lensId": 9
			}
			""";

	@Mock
	private OrderService service;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(this.service))
			.setControllerAdvice(new OrderExceptionHandler())
			.build();
	}

	@Test
	void acceptsAnOrderAndEchoesTheSelectedType() throws Exception {
		when(this.service.create(any())).thenReturn(order());

		this.mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(3L))
			.andExpect(jsonPath("$.data.customerId").value(7L))
			.andExpect(jsonPath("$.data.prescriptionId").value(12L))
			.andExpect(jsonPath("$.data.frameId").value(4L))
			.andExpect(jsonPath("$.data.lensId").value(9L))
			.andExpect(jsonPath("$.data.orderType").value("PROGRESSIVE"))
			.andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	/**
	 * No receive date has been calculated yet, so the field is absent rather than
	 * carrying a value the shop never promised.
	 */
	@Test
	void omitsTheReceiveDateUntilTheShopHasQuotedOne() throws Exception {
		Order unquoted = order();
		unquoted.setReceiveDate(null);
		when(this.service.create(any())).thenReturn(unquoted);

		this.mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.receiveDate").doesNotExist());
	}

	@Test
	void carriesTheReceiveDateOnceTheShopHasQuotedOne() throws Exception {
		when(this.service.create(any())).thenReturn(order());

		this.mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.receiveDate").value("2026-10-20"));
	}

	@Test
	void acceptsAnOrderThatNamesTheFrameAndLens() throws Exception {
		when(this.service.create(any())).thenReturn(order());

		this.mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{ "prescriptionId": 12, "orderType": "BIFOCAL" }
				""")).andExpect(status().isCreated());
	}

	@Test
	void rejectsAnOrderWithoutAPrescription() throws Exception {
		this.mockMvc
			.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
					{ "orderType": "PROGRESSIVE" }
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.error.fieldErrors.prescriptionId").exists());
	}

	/**
	 * The order type is the one field with no sensible default, because defaulting
	 * it would decide for the customer which workflow their order enters.
	 */
	@Test
	void rejectsAnOrderWithoutAnOrderType() throws Exception {
		this.mockMvc
			.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
					{ "prescriptionId": 12 }
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.error.fieldErrors.orderType").exists());
	}

	@Test
	void rejectsAnOrderTypeItDoesNotRecognise() throws Exception {
		this.mockMvc
			.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
					{ "prescriptionId": 12, "orderType": "progressiv" }
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void rejectsANonPositivePrescriptionId() throws Exception {
		this.mockMvc
			.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
					{ "prescriptionId": 0, "orderType": "SINGLE_VISION" }
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.error.fieldErrors.prescriptionId").exists());
	}

	@Test
	void reportsAnOrderTypeThatDoesNotFitThePrescription() throws Exception {
		when(this.service.create(any())).thenThrow(new InvalidOrderException("Some order values are not valid",
				Map.of("orderType", "a progressive lens needs a prescription with a near addition on both eyes")));

		this.mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_ORDER"))
			.andExpect(jsonPath("$.error.fieldErrors.orderType").exists());
	}

	@Test
	void reportsAPrescriptionThatDoesNotExist() throws Exception {
		when(this.service.create(any())).thenThrow(new PrescriptionNotFoundException(404L));

		this.mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("PRESCRIPTION_NOT_FOUND"));
	}

	@Test
	void reportsASecondOrderForTheSamePrescriptionAsAConflict() throws Exception {
		when(this.service.create(any())).thenThrow(new OrderAlreadyExistsException(12L));

		this.mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("ORDER_ALREADY_EXISTS"))
			.andExpect(jsonPath("$.error.fieldErrors.prescriptionId").exists());
	}

	@Test
	void returnsASingleOrder() throws Exception {
		when(this.service.getById(3L)).thenReturn(order());

		this.mockMvc.perform(get("/api/orders/3"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(3L))
			.andExpect(jsonPath("$.data.orderType").value("PROGRESSIVE"));
	}

	@Test
	void reportsAnUnknownOrder() throws Exception {
		when(this.service.getById(99L)).thenThrow(new OrderNotFoundException(99L));

		this.mockMvc.perform(get("/api/orders/99"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"))
			.andExpect(jsonPath("$.error.message").value("Order 99 was not found"));
	}

	/**
	 * The other direction of the link, so a customer holding a prescription can
	 * find the order made from it.
	 */
	@Test
	void findsTheOrderBuiltFromAPrescription() throws Exception {
		when(this.service.getByPrescriptionId(12L)).thenReturn(order());

		this.mockMvc.perform(get("/api/orders").param("prescriptionId", "12"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.prescriptionId").value(12L));
	}

	@Test
	void reportsAPrescriptionThatHasNotBeenOrdered() throws Exception {
		when(this.service.getByPrescriptionId(12L))
			.thenThrow(new OrderNotFoundException("Prescription 12 has not been ordered yet"));

		this.mockMvc.perform(get("/api/orders").param("prescriptionId", "12"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"))
			.andExpect(jsonPath("$.error.message").value("Prescription 12 has not been ordered yet"));
	}

	@Test
	void refusesALookupWithoutAPrescriptionId() throws Exception {
		this.mockMvc.perform(get("/api/orders"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void refusesANonNumericOrderId() throws Exception {
		this.mockMvc.perform(get("/api/orders/abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
	}

	private static Order order() {
		Order order = new Order();
		order.setOrderId(3L);
		order.setCustomerId(7L);
		order.setPrescriptionId(12L);
		order.setFrameId(4L);
		order.setLensId(9L);
		order.setOrderType(OrderType.PROGRESSIVE);
		order.setStatus(OrderStatus.PENDING_REVIEW);
		order.setReceiveDate(LocalDate.of(2026, 10, 20));
		order.setCreatedAt(Instant.parse("2026-09-29T10:15:30Z"));
		order.setUpdatedAt(Instant.parse("2026-09-29T10:15:30Z"));
		return order;
	}

}
