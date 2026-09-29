package com.myopty.order.service;

import java.util.List;

import com.myopty.order.domain.Order;
import com.myopty.order.domain.OrderNotification;
import com.myopty.order.domain.OrderStatus;

/**
 * Keeps the customer told about their own order.
 *
 * <p>Separate from {@link OrderService} because the order service's job is to
 * decide whether a step is legal, and this one is told the answer. Splitting them
 * keeps the workflow free of any knowledge of how a customer is contacted, which
 * is the part most likely to be replaced.
 */
public interface OrderNotificationService {

	/**
	 * Records that an order moved from one status to another, and tells the
	 * customer.
	 *
	 * <p>Must be called from inside the transaction that made the move, so a
	 * notification cannot outlive a status change that then failed.
	 *
	 * @param order the order as it now stands, used for the order and customer ids
	 * @param from  the status it was in, kept so the history reads as a sequence
	 * @param to    the status it is in now
	 * @return the notification that was stored
	 */
	OrderNotification record(Order order, OrderStatus from, OrderStatus to);

	/**
	 * Everything the shop has told the customer about one order, newest first.
	 *
	 * @throws com.myopty.order.exception.InvalidOrderException if no order id is given
	 */
	List<OrderNotification> listForOrder(Long orderId);

	/**
	 * Everything the shop has told one customer across all of their orders, newest
	 * first.
	 *
	 * @throws com.myopty.order.exception.InvalidOrderException if no customer id is given
	 */
	List<OrderNotification> listForCustomer(Long customerId);

}
