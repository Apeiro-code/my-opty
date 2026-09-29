package com.myopty.order.controller;

import java.util.List;

import com.myopty.order.dto.ApiResponse;
import com.myopty.order.dto.NotificationResponse;
import com.myopty.order.mapper.NotificationMapper;
import com.myopty.order.service.OrderNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reads the notifications the shop has sent about an order.
 *
 * <p>A separate controller from {@link OrderController} because these are not
 * orders: they are what the shop said about them, and they are read by a
 * customer rather than by the client working the queue.
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "What the shop has told a customer about their orders")
public class NotificationController {

	private final OrderNotificationService service;

	public NotificationController(OrderNotificationService service) {
		this.service = service;
	}

	/**
	 * One read serves both callers rather than there being a customer route and a
	 * client route, because the data is the same and the difference is only whose
	 * id is being looked up.
	 *
	 * <p>At least one filter is required. Without authentication there is no way to
	 * tell who is asking, so "no filter" cannot mean "the caller's own
	 * notifications" the way it would once the shared module owns identity: it
	 * would mean the entire table, which is every customer's order history to
	 * anyone who asks. Refusing it is not a substitute for authentication, and the
	 * route is still open to anyone who supplies an id, but it does stop the
	 * unbounded read that would otherwise be the easiest thing to abuse.
	 */
	@Operation(summary = "List the notifications about an order or a customer",
			description = "Returns what the shop has told the customer, newest first, at most 100 entries. Supply "
					+ "customerId for everything about one customer's orders, or orderId for one order's history. At "
					+ "least one is required. The order filters are meant for the client; the customer filter is what a "
					+ "customer reads their own account with, and needs the authentication this module does not have "
					+ "yet to be safe.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
				description = "Notifications, newest first"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
				description = "Neither customerId nor orderId was given") })
	@GetMapping
	public ApiResponse<List<NotificationResponse>> list(
			@Parameter(description = "Customer to list notifications for; ignored when orderId is given") //
			@RequestParam(required = false) Long customerId,
			@Parameter(description = "Order to list notifications for; takes precedence over customerId") //
			@RequestParam(required = false) Long orderId) {
		// The order filter wins when both are sent, because it is the narrower
		// question and answering it with a whole customer's history when the caller
		// asked about one order would be the wrong answer to a reasonable request.
		List<NotificationResponse> notifications = (orderId != null ? this.service.listForOrder(orderId)
				: this.service.listForCustomer(customerId)).stream().map(NotificationMapper::toResponse).toList();
		return ApiResponse.ok(notifications);
	}

}
