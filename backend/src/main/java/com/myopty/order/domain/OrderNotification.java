package com.myopty.order.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Something the customer was told about one of their orders.
 *
 * <p>Written once, when the order moves, and never edited afterwards. That is the
 * whole point of keeping it: the row is the record of what the customer was told
 * and when, so a later change to how notifications are worded cannot rewrite
 * history. The message is stored for the same reason.
 *
 * <p>Both statuses are kept rather than just the new one, so the history reads as
 * "approved, then ready" instead of an unordered list of states.
 */
@Table("order_notification")
public class OrderNotification {

	@Id
	private Long notificationId;

	/**
	 * The order this is about. Deliberately not a {@link Order} reference: the
	 * notification has to stay readable on its own, without loading the order it
	 * describes, which by the time a customer reads it may have been tidied away.
	 */
	private Long orderId;

	/**
	 * The customer as the order recorded them, which may be null. Copied rather
	 * than joined so the notification still says who it was for after the order's
	 * own customer link has been corrected.
	 */
	private Long customerId;

	private OrderStatus fromStatus;

	private OrderStatus toStatus;

	/**
	 * The text the customer reads. Kept as it was written rather than rendered on
	 * read, so rewording the templates does not change what past notifications
	 * claim was said.
	 */
	private String message;

	private Instant createdAt;

	public Long getNotificationId() {
		return this.notificationId;
	}

	public void setNotificationId(Long notificationId) {
		this.notificationId = notificationId;
	}

	public Long getOrderId() {
		return this.orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public Long getCustomerId() {
		return this.customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}

	public OrderStatus getFromStatus() {
		return this.fromStatus;
	}

	public void setFromStatus(OrderStatus fromStatus) {
		this.fromStatus = fromStatus;
	}

	public OrderStatus getToStatus() {
		return this.toStatus;
	}

	public void setToStatus(OrderStatus toStatus) {
		this.toStatus = toStatus;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

}
