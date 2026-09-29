package com.myopty.order.controller;

import com.myopty.order.domain.Order;
import com.myopty.order.dto.ApiResponse;
import com.myopty.order.dto.OrderCreateRequest;
import com.myopty.order.dto.OrderResponse;
import com.myopty.order.mapper.OrderMapper;
import com.myopty.order.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order endpoints for the Order &amp; Prescription module.
 *
 * <p>Mounted at {@code /api/orders} rather than the {@code /api/orders/progressive}
 * of the original plan, because the order type is now something the customer
 * selects and a path segment that spells it out would fix the value the URL was
 * supposed to leave open.
 *
 * <p>The Swagger {@code io.swagger...ApiResponse} annotation clashes with the
 * {@link ApiResponse} envelope, so it is referenced by its fully qualified name.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Place and retrieve customer orders for a selected lens type")
public class OrderController {

	private final OrderService service;

	public OrderController(OrderService service) {
		this.service = service;
	}

	@Operation(summary = "Place an order against a prescription",
			description = "Links the order to an existing prescription and routes it by the selected order type. "
					+ "The order is created with status PENDING_REVIEW for the shop to approve. A prescription can "
					+ "only be ordered once, and a bifocal or progressive order needs a prescription that carries a "
					+ "near addition on both eyes.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order placed"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
					description = "Order type does not fit the prescription, or the customer does not own it"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
					description = "Prescription not found"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
					description = "That prescription has already been ordered") })
	@PostMapping
	public ResponseEntity<ApiResponse<OrderResponse>> create(
			@Parameter(description = "Order to place", required = true) //
			@Valid @RequestBody OrderCreateRequest request) {
		Order created = this.service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(OrderMapper.toResponse(created)));
	}

	@Operation(summary = "View an order")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order found"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found") })
	@GetMapping("/{orderId}")
	public ApiResponse<OrderResponse> getById(
			@Parameter(description = "Order id", required = true) @PathVariable Long orderId) {
		return ApiResponse.ok(OrderMapper.toResponse(this.service.getById(orderId)));
	}

	@Operation(summary = "View the order built from a prescription",
			description = "The other direction of the same link: given the prescription a customer already holds, "
					+ "this is how they find the order that was made from it.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order found"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
					description = "Order not found, or that prescription has not been ordered") })
	@GetMapping
	public ApiResponse<OrderResponse> getByPrescriptionId(
			@Parameter(description = "Prescription the order was built from", required = true) //
			@RequestParam Long prescriptionId) {
		return ApiResponse.ok(OrderMapper.toResponse(this.service.getByPrescriptionId(prescriptionId)));
	}

}
