package com.myopty.order.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import com.myopty.order.domain.Order;
import com.myopty.order.domain.OrderNotification;
import com.myopty.order.domain.OrderStatus;
import com.myopty.order.exception.InvalidOrderException;
import com.myopty.order.repository.OrderNotificationRepository;
import com.myopty.order.service.notification.NotificationSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link OrderNotificationService}.
 */
@Service
public class OrderNotificationServiceImpl implements OrderNotificationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderNotificationServiceImpl.class);

	/**
	 * What the customer is told when their order reaches each state.
	 *
	 * <p>Wording lives here, in one map, rather than being built at each call site.
	 * Five steps that all say the same kind of thing are exactly the kind of copy
	 * that drifts: three of them end up "ready for collection", "ready for you to
	 * collect" and "your order is ready". Collecting them also means a new status
	 * cannot be added without this being noticed, which
	 * {@code rendersAMessageForEveryStatusTheCustomerIsToldAbout} checks.
	 *
	 * <p>Deliberately says what happened rather than what the shop is doing next.
	 * The customer did not ask to be told the order is in the lab, and a message
	 * that promises a date the lab has not committed to is the sort of thing that
	 * becomes a complaint.
	 */
	private static final Map<OrderStatus, String> CUSTOMER_MESSAGE = Map.of(OrderStatus.APPROVED,
			"Your order has been accepted. We will let you know when it is ready.", OrderStatus.REJECTED,
			"Sorry, we are not able to make this order. The reason is with your order.",
			OrderStatus.PROCESSING, "Your order is now being made.", OrderStatus.READY,
			"Your order is ready to collect.", OrderStatus.DISPATCHED,
			"Your order has been handed over or sent to you.");

	private final OrderNotificationRepository repository;

	private final NotificationSender sender;

	private final Clock clock;

	public OrderNotificationServiceImpl(OrderNotificationRepository repository, NotificationSender sender, Clock clock) {
		this.repository = repository;
		this.sender = sender;
		this.clock = clock;
	}

	/**
	 * MANDATORY rather than the default, and that is the interesting part of this
	 * method.
	 *
	 * <p>Left to itself, Spring Data would open a transaction for the insert, and
	 * that transaction would commit even if the order status change that prompted
	 * it was rolled back a moment later. The customer would then be owed a
	 * notification about a move that never happened. Requiring an existing
	 * transaction means the two writes commit or fail together.
	 */
	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public OrderNotification record(Order order, OrderStatus from, OrderStatus to) {
		OrderNotification notification = new OrderNotification();
		notification.setOrderId(order.getOrderId());
		// Copied from the order rather than looked up: the notification has to say
		// who it was for as things were when it was written, and reading the
		// customer's id again would be a second source of truth to keep in step.
		notification.setCustomerId(order.getCustomerId());
		notification.setFromStatus(from);
		notification.setToStatus(to);
		notification.setMessage(messageFor(to));
		// The injected clock rather than Instant.now(), so the recorded time is
		// something a test can assert instead of something it has to be relaxed
		// around.
		notification.setCreatedAt(this.clock.instant());

		OrderNotification saved = this.repository.save(notification);
		deliver(saved);
		return saved;
	}

	/**
	 * Hands the notification on, and lets it fail.
	 *
	 * <p>The catch is the point. This runs inside the transaction that just moved
	 * the order, and a customer not receiving an email is not a reason to un-approve
	 * their lenses or to reject a change the shop has already made. The row is
	 * already stored, so the notification is not lost either: it stays in
	 * {@code order_notification} to be picked up by whatever retries it.
	 *
	 * <p>A sender that talks to a network should declare itself
	 * {@code NOT_SUPPORTED} or otherwise run after the commit, so that a slow or
	 * unreachable provider cannot hold the shop's database open while it waits.
	 */
	private void deliver(OrderNotification notification) {
		try {
			this.sender.send(notification);
		}
		catch (RuntimeException ex) {
			LOGGER.warn("Could not deliver notification {} for order {}. The order moved anyway and the notification "
					+ "is stored for a later attempt.", notification.getNotificationId(), notification.getOrderId(), ex);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<OrderNotification> listForOrder(Long orderId) {
		if (orderId == null) {
			throw new InvalidOrderException("A notification read has to say whose notifications they are",
					Map.of("orderId", "is required", "customerId", "is required"));
		}
		return this.repository.findTop100ByOrderIdOrderByCreatedAtDescOrderIdDesc(orderId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<OrderNotification> listForCustomer(Long customerId) {
		if (customerId == null) {
			throw new InvalidOrderException("A notification read has to say whose notifications they are",
					Map.of("orderId", "is required", "customerId", "is required"));
		}
		return this.repository.findTop100ByCustomerIdOrderByCreatedAtDescOrderIdDesc(customerId);
	}

	/**
	 * @throws IllegalArgumentException for a status the customer is never told
	 *                                  about, which is
	 *                                  {@link OrderStatus#PENDING_REVIEW}: nobody is
	 *                                  notified that an order has been placed, because
	 *                                  placing it is not news.
	 */
	private static String messageFor(OrderStatus to) {
		String message = CUSTOMER_MESSAGE.get(to);
		if (message == null) {
			throw new IllegalArgumentException("No customer message for " + to);
		}
		return message;
	}

}
