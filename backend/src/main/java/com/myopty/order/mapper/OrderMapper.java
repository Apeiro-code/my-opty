package com.myopty.order.mapper;

import com.myopty.order.domain.Order;
import com.myopty.order.dto.OrderResponse;

/**
 * Converts a stored {@link Order} into its API representation. The prescription is
 * reported by id only: the order is its own aggregate, and embedding the whole
 * prescription would drag a second load into every read of an order.
 */
public final class OrderMapper {

	private OrderMapper() {
	}

	public static OrderResponse toResponse(Order order) {
		return new OrderResponse(order.getOrderId(), order.getCustomerId(), order.getPrescriptionId(), order.getFrameId(),
				order.getLensId(), nameOf(order.getOrderType()), nameOf(order.getStatus()), order.getReceiveDate(),
				order.getCreatedAt(), order.getUpdatedAt());
	}

	/**
	 * A row read back before it was written can still have a null enum, which
	 * {@code enum::name} would turn into a 500 on the way out.
	 */
	private static String nameOf(Enum<?> value) {
		return value == null ? null : value.name();
	}

}
