package com.myopty.order.service.notification;

import com.myopty.order.domain.OrderNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The sender used until a real one exists: writes the notification to the log.
 *
 * <p>Deliberately not doing nothing. A log line is the only way to see that the
 * workflow is producing the notifications it is supposed to, and it is what makes
 * a bug in the wording visible before a customer is the one to report it.
 *
 * <p>Not a {@code @Component}. The bean is declared in
 * {@code NotificationConfig} behind a {@code @ConditionalOnMissingBean}, because a
 * second {@link NotificationSender} scanned in would otherwise leave the
 * injection point ambiguous and fail at startup. Going through a configuration
 * class is what makes the condition dependable; a condition on a scanned component
 * depends on scan order.
 */
public class LoggingNotificationSender implements NotificationSender {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingNotificationSender.class);

	@Override
	public void send(OrderNotification notification) {
		LOGGER.info("Notification {} for order {} (customer {}): {} -> {}: {}", notification.getNotificationId(),
				notification.getOrderId(), notification.getCustomerId(), notification.getFromStatus(),
				notification.getToStatus(), notification.getMessage());
	}

}
