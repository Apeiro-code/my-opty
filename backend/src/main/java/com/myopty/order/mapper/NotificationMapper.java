package com.myopty.order.mapper;

import com.myopty.order.domain.OrderNotification;
import com.myopty.order.dto.NotificationResponse;

/**
 * Converts a stored {@link OrderNotification} into its API representation.
 */
public final class NotificationMapper {

	private NotificationMapper() {
	}

	public static NotificationResponse toResponse(OrderNotification notification) {
		return new NotificationResponse(notification.getNotificationId(), notification.getOrderId(),
				notification.getCustomerId(), nameOf(notification.getFromStatus()), nameOf(notification.getToStatus()),
				notification.getMessage(), notification.getCreatedAt());
	}

	/**
	 * A row read back before it was written can still have a null enum, which
	 * {@code enum::name} would turn into a 500 on the way out.
	 */
	private static String nameOf(Enum<?> value) {
		return value == null ? null : value.name();
	}

}
