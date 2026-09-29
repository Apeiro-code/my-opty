package com.myopty.order.service;

import com.myopty.order.domain.Order;
import com.myopty.order.dto.OrderCreateRequest;

/**
 * Order placement and retrieval for the Order &amp; Prescription module.
 */
public interface OrderService {

	/**
	 * Places an order for the selected lens type against an existing prescription.
	 *
	 * <p>The order starts at {@code PENDING_REVIEW}: a customer chooses the order
	 * type but never the workflow state, and the prescription's own status is left
	 * untouched because the shop reviews it separately.
	 *
	 * @param request the order type the customer selected, the prescription to build
	 *                from and, when the catalog knows them, the frame and lens
	 * @throws com.myopty.order.exception.PrescriptionNotFoundException if the prescription does not exist
	 * @throws com.myopty.order.exception.OrderAlreadyExistsException   if that prescription has already been ordered
	 * @throws com.myopty.order.exception.InvalidOrderException         if the order type does not fit the prescription
	 */
	Order create(OrderCreateRequest request);

	/**
	 * @throws com.myopty.order.exception.OrderNotFoundException if no such order exists
	 */
	Order getById(Long orderId);

	/**
	 * Looks the order up from the prescription it was built from, which is how the
	 * customer finds the order for a prescription they already hold.
	 *
	 * @throws com.myopty.order.exception.OrderNotFoundException if that prescription has no order
	 */
	Order getByPrescriptionId(Long prescriptionId);

}
