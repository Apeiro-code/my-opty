package com.myopty.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import com.myopty.order.domain.Order;
import com.myopty.order.domain.OrderNotification;
import com.myopty.order.domain.OrderStatus;
import com.myopty.order.exception.InvalidOrderException;
import com.myopty.order.repository.OrderNotificationRepository;
import com.myopty.order.service.notification.NotificationSender;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Recording what the customer was told, and telling them.
 */
@ExtendWith(MockitoExtension.class)
class OrderNotificationServiceImplTest {

	private static final Long ORDER_ID = 3L;

	private static final Long CUSTOMER_ID = 7L;

	/**
	 * Frozen so the recorded time is a value the test can state outright, for the
	 * same reason the order service freezes it for the quoted receive date.
	 */
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T09:00:00Z"), ZoneOffset.UTC);

	@Mock
	private OrderNotificationRepository repository;

	@Mock
	private NotificationSender sender;

	@Captor
	private ArgumentCaptor<OrderNotification> saved;

	private OrderNotificationServiceImpl service() {
		return new OrderNotificationServiceImpl(this.repository, this.sender, CLOCK);
	}

	private static Order order() {
		Order order = new Order();
		order.setOrderId(ORDER_ID);
		order.setCustomerId(CUSTOMER_ID);
		order.setStatus(OrderStatus.APPROVED);
		return order;
	}

	@Test
	void recordsWhichWayTheOrderMoved() {
		when(this.repository.save(any())).thenAnswer(call -> call.getArgument(0));

		OrderNotification recorded = service().record(order(), OrderStatus.PENDING_REVIEW, OrderStatus.APPROVED);

		assertThat(recorded.getOrderId()).isEqualTo(ORDER_ID);
		assertThat(recorded.getFromStatus()).isEqualTo(OrderStatus.PENDING_REVIEW);
		assertThat(recorded.getToStatus()).isEqualTo(OrderStatus.APPROVED);
	}

	/**
	 * Copied off the order rather than looked up, because the notification has to
	 * keep saying who it was for even if the order's own customer link is later
	 * corrected.
	 */
	@Test
	void copiesTheCustomerFromTheOrder() {
		when(this.repository.save(any())).thenAnswer(call -> call.getArgument(0));

		OrderNotification recorded = service().record(order(), OrderStatus.APPROVED, OrderStatus.READY);

		assertThat(recorded.getCustomerId()).isEqualTo(CUSTOMER_ID);
	}

	/**
	 * Most orders have no customer id at all until the shared module lands, and the
	 * notification still has to be written. Writing it is what leaves something to
	 * backfill from later.
	 */
	@Test
	void recordsANotificationForAnOrderWithNoCustomer() {
		Order orderless = order();
		orderless.setCustomerId(null);
		when(this.repository.save(any())).thenAnswer(call -> call.getArgument(0));

		OrderNotification recorded = service().record(orderless, OrderStatus.APPROVED, OrderStatus.PROCESSING);

		assertThat(recorded.getCustomerId()).isNull();
		assertThat(recorded.getOrderId()).isEqualTo(ORDER_ID);
	}

	@Test
	void recordsWhenTheOrderMovedRatherThanWhenTheOrderArrived() {
		when(this.repository.save(any())).thenAnswer(call -> call.getArgument(0));

		OrderNotification recorded = service().record(order(), OrderStatus.APPROVED, OrderStatus.READY);

		assertThat(recorded.getCreatedAt()).isEqualTo(Instant.parse("2026-03-10T09:00:00Z"));
	}

	@Test
	void sendsWhatItRecorded() {
		when(this.repository.save(any())).thenAnswer(call -> call.getArgument(0));

		OrderNotification recorded = service().record(order(), OrderStatus.READY, OrderStatus.DISPATCHED);

		verify(this.sender).send(recorded);
	}

	/**
	 * The reason the delivery is wrapped. A customer not receiving an email is not a
	 * reason to un-approve their lenses, and the row has already been written, so
	 * the notification is not lost either.
	 */
	@Test
	void doesNotFailTheOrderWhenTheSenderBreaks() {
		doThrow(new IllegalStateException("the mail provider is down")).when(this.sender).send(any());
		when(this.repository.save(any())).thenAnswer(call -> call.getArgument(0));

		OrderNotification recorded = service().record(order(), OrderStatus.APPROVED, OrderStatus.READY);

		assertThat(recorded.getToStatus()).isEqualTo(OrderStatus.READY);
		verify(this.repository).save(any());
	}

	/**
	 * A sender that quietly swallowed its own failures would be reported as a
	 * working notification system. The row is stored either way, so this only proves
	 * the write happened.
	 */
	@Test
	void stillStoresTheNotificationWhenDeliveryFails() {
		doThrow(new IllegalStateException("provider down")).when(this.sender).send(any());
		when(this.repository.save(any())).thenAnswer(call -> call.getArgument(0));

		service().record(order(), OrderStatus.APPROVED, OrderStatus.READY);

		verify(this.repository).save(this.saved.capture());
		assertThat(this.saved.getValue().getMessage()).isNotBlank();
	}

	/**
	 * Every status the customer is told about has to have something to say. A status
	 * added later without copy would otherwise fail at runtime, on the one request
	 * that tries to use it, with an exception rather than a notification.
	 */
	@Test
	void hasAMessageForEveryStatusTheCustomerIsToldAbout() {
		List<OrderStatus> expected = List.of(OrderStatus.APPROVED, OrderStatus.REJECTED, OrderStatus.PROCESSING,
				OrderStatus.READY, OrderStatus.DISPATCHED);

		for (OrderStatus status : expected) {
			when(this.repository.save(any())).thenAnswer(call -> call.getArgument(0));

			OrderNotification recorded = service().record(order(), OrderStatus.PENDING_REVIEW, status);

			assertThat(recorded.getMessage()).as("message for %s", status).isNotBlank();
		}
	}

	/**
	 * Placing an order is not news, so nothing is sent for it. This states that as a
	 * decision rather than leaving it to be discovered, because a
	 * "message for everything" default would quietly start emailing customers about
	 * their own orders.
	 */
	@Test
	void hasNothingToSayAboutAnOrderStillAwaitingReview() {
		Order order = order();
		order.setStatus(OrderStatus.PENDING_REVIEW);

		assertThatThrownBy(() -> service().record(order, OrderStatus.PENDING_REVIEW, OrderStatus.PENDING_REVIEW))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("PENDING_REVIEW");

		// Nothing is stored for a status with no message: a row with a blank message
		// is worse than no row, because it looks like a notification that was meant
		// to say something.
		verifyNoInteractions(this.repository);
	}

	/**
	 * The statuses the customer is told about are the five the workflow moves
	 * through, and nothing else. Written as a set comparison so that a new status
	 * forces the question "is this news for the customer?" to be answered.
	 */
	@Test
	void coversExactlyTheStatusesThatAreNews() {
		List<OrderStatus> rendered = Arrays.stream(OrderStatus.values())
			.filter(status -> rendersFor(status))
			.toList();

		assertThat(rendered).containsExactlyInAnyOrder(OrderStatus.APPROVED, OrderStatus.REJECTED, OrderStatus.PROCESSING,
				OrderStatus.READY, OrderStatus.DISPATCHED);
	}

	private boolean rendersFor(OrderStatus status) {
		when(this.repository.save(any())).thenAnswer(call -> call.getArgument(0));
		try {
			service().record(order(), OrderStatus.PENDING_REVIEW, status);
			return true;
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

	@Test
	void listsOneOrdersNotifications() {
		when(this.repository.findTop100ByOrderIdOrderByCreatedAtDescOrderIdDesc(ORDER_ID))
			.thenReturn(List.of(notification(1L, ORDER_ID)));

		List<OrderNotification> found = service().listForOrder(ORDER_ID);

		assertThat(found).hasSize(1);
		verify(this.repository).findTop100ByOrderIdOrderByCreatedAtDescOrderIdDesc(ORDER_ID);
	}

	@Test
	void listsOneCustomersNotifications() {
		when(this.repository.findTop100ByCustomerIdOrderByCreatedAtDescOrderIdDesc(CUSTOMER_ID))
			.thenReturn(List.of(notification(1L, ORDER_ID)));

		List<OrderNotification> found = service().listForCustomer(CUSTOMER_ID);

		assertThat(found).hasSize(1);
		verify(this.repository).findTop100ByCustomerIdOrderByCreatedAtDescOrderIdDesc(CUSTOMER_ID);
	}

	/**
	 * There is no "my notifications" route and no way to know who is asking, so
	 * asking for nothing at all has to be refused rather than answered with
	 * everything in the table.
	 */
	@Test
	void refusesToListNotificationsForNobody() {
		assertThatThrownBy(() -> service().listForOrder(null)).isInstanceOf(InvalidOrderException.class)
			.hasMessageContaining("whose notifications");

		assertThatThrownBy(() -> service().listForCustomer(null)).isInstanceOf(InvalidOrderException.class)
			.hasMessageContaining("whose notifications");

		verifyNoInteractions(this.repository);
	}

	private static OrderNotification notification(Long id, Long orderId) {
		OrderNotification notification = new OrderNotification();
		notification.setNotificationId(id);
		notification.setOrderId(orderId);
		notification.setFromStatus(OrderStatus.APPROVED);
		notification.setToStatus(OrderStatus.READY);
		notification.setMessage("Your order is ready to collect.");
		notification.setCreatedAt(Instant.parse("2026-03-10T09:00:00Z"));
		return notification;
	}

}
