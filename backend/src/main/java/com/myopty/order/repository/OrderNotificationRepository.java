package com.myopty.order.repository;

import java.util.List;

import com.myopty.order.domain.OrderNotification;

import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC access to the {@code order_notification} table, owned by the
 * Order &amp; Prescription module.
 */
public interface OrderNotificationRepository extends CrudRepository<OrderNotification, Long> {

	/**
	 * Everything the shop has told the customer about one order, newest first.
	 *
	 * <p>Newest first because the question being asked is almost always "what has
	 * happened to my order now", and the current state is the most recent row.
	 * {@code OrderId} breaks ties within a timestamp so repeated calls return the
	 * same order, which matters because {@code created_at} is a
	 * {@code DATETIME(6)} and two steps taken in the same microsecond would
	 * otherwise come back in either order.
	 *
	 * <p>Capped at 100 for the same reason the client's approval queue is: an order
	 * that has somehow accumulated hundreds of rows should not be able to make
	 * this read expensive for whoever is asking.
	 */
	List<OrderNotification> findTop100ByOrderIdOrderByCreatedAtDescOrderIdDesc(Long orderId);

	/**
	 * Everything the shop has told one customer, across all of their orders, newest
	 * first. This is the read a customer makes of their own account, so it is the
	 * one most likely to grow without bound.
	 *
	 * <p>Derived from the method name, so the property has to keep being called
	 * {@code customerId}. Serves {@code idx_order_notification_customer}.
	 */
	List<OrderNotification> findTop100ByCustomerIdOrderByCreatedAtDescOrderIdDesc(Long customerId);

}
