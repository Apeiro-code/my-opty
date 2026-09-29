package com.myopty.order.service.notification;

import com.myopty.order.domain.OrderNotification;

/**
 * How a notification actually reaches the customer.
 *
 * <p>The interface exists so the shop can change how it talks to customers without
 * the workflow knowing. Right now the only implementation writes to the log,
 * because sending mail needs a provider and credentials the shared module has not
 * got yet; wiring a real transport in later means declaring another bean, which
 * displaces the log one without a code change, since it is declared behind a
 * {@code @ConditionalOnMissingBean} in {@code NotificationConfig}.
 *
 * <p>Following the shape of {@code DocumentStorage} rather than inventing a
 * second one, so both pluggable edges of this module read the same way.
 *
 * <p><strong>Implementations must not fail the order.</strong> This is called from
 * inside the transaction that moves the order, and a customer not receiving an
 * email is not a reason to un-approve their lenses. The caller catches and logs
 * anything thrown, so an implementation is free to be simple, but a sender that
 * needs to talk to a network should expect to be called before the transaction
 * has committed and should not assume the order row is visible to anyone else
 * yet.
 */
public interface NotificationSender {

	/**
	 * Delivers one notification. Called once per status change, after the row has
	 * been written.
	 *
	 * @param notification the notification to deliver, as it was stored
	 */
	void send(OrderNotification notification);

}
