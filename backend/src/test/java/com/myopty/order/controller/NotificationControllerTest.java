package com.myopty.order.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.myopty.order.domain.OrderNotification;
import com.myopty.order.domain.OrderStatus;
import com.myopty.order.exception.InvalidOrderException;
import com.myopty.order.exception.OrderExceptionHandler;
import com.myopty.order.service.OrderNotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * The read a customer makes of what the shop has told them.
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

	@Mock
	private OrderNotificationService service;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(this.service))
			.setControllerAdvice(new OrderExceptionHandler())
			.build();
	}

	private static OrderNotification notification(Long id, Long orderId, Long customerId) {
		OrderNotification notification = new OrderNotification();
		notification.setNotificationId(id);
		notification.setOrderId(orderId);
		notification.setCustomerId(customerId);
		notification.setFromStatus(OrderStatus.PROCESSING);
		notification.setToStatus(OrderStatus.READY);
		notification.setMessage("Your order is ready to collect.");
		notification.setCreatedAt(Instant.parse("2026-03-10T09:00:00Z"));
		return notification;
	}

	@Test
	void listsACustomersNotifications() throws Exception {
		when(this.service.listForCustomer(7L)).thenReturn(List.of(notification(1L, 3L, 7L)));

		this.mockMvc.perform(get("/api/notifications").param("customerId", "7"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].orderId").value(3))
			.andExpect(jsonPath("$.data[0].customerId").value(7))
			.andExpect(jsonPath("$.data[0].fromStatus").value("PROCESSING"))
			.andExpect(jsonPath("$.data[0].toStatus").value("READY"))
			.andExpect(jsonPath("$.data[0].message").value("Your order is ready to collect."));
	}

	@Test
	void listsOneOrdersNotifications() throws Exception {
		when(this.service.listForOrder(3L)).thenReturn(List.of(notification(1L, 3L, 7L)));

		this.mockMvc.perform(get("/api/notifications").param("orderId", "3"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].orderId").value(3));
	}

	/**
	 * Answering "about this order" with the customer's whole history because both
	 * were sent would be the wrong answer to a reasonable request, and the order
	 * filter is the narrower question.
	 */
	@Test
	void answersForTheOrderWhenBothFiltersAreGiven() throws Exception {
		when(this.service.listForOrder(3L)).thenReturn(List.of());

		this.mockMvc.perform(get("/api/notifications").param("orderId", "3").param("customerId", "7"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(0));

		verify(this.service).listForOrder(3L);
	}

	/**
	 * Without a filter there is nothing to narrow to, and with no authentication
	 * there is no way to guess whose notifications "mine" would mean, so the answer
	 * would be the whole table.
	 */
	@Test
	void refusesAnUnfilteredRead() throws Exception {
		when(this.service.listForCustomer(null)).thenThrow(
				new InvalidOrderException("A notification read has to say whose notifications they are",
						Map.of("orderId", "is required", "customerId", "is required")));

		this.mockMvc.perform(get("/api/notifications"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_ORDER"))
			.andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("whose notifications")))
			.andExpect(jsonPath("$.error.fieldErrors.orderId").value("is required"));
	}

	/**
	 * The controller passes the unfiltered request straight through rather than
	 * checking it itself. The rule is in the service, so there is exactly one place
	 * that can answer "whose notifications are these", and a second check here
	 * would be one more thing to keep in step with it.
	 */
	@Test
	void leavesTheUnfilteredReadToTheService() throws Exception {
		this.mockMvc.perform(get("/api/notifications")).andExpect(status().isOk());

		verify(this.service).listForCustomer(null);
	}

	/**
	 * A customer with nothing to show still gets a successful read, not a 404. "You
	 * have no notifications" is a true answer to the question asked.
	 */
	@Test
	void reportsNoNotificationsAsAnEmptyList() throws Exception {
		when(this.service.listForCustomer(7L)).thenReturn(List.of());

		this.mockMvc.perform(get("/api/notifications").param("customerId", "7"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(0));
	}

	/**
	 * The order of the two filters is also worth pinning: {@code orderId} is checked
	 * first, so a caller sending both gets the narrow answer.
	 */
	@Test
	void prefersTheOrderFilter() throws Exception {
		when(this.service.listForOrder(3L)).thenReturn(List.of());

		this.mockMvc.perform(get("/api/notifications").param("customerId", "7").param("orderId", "3"))
			.andExpect(status().isOk());

		verify(this.service).listForOrder(3L);
	}

}
