package com.myopty.order.repository;

import java.util.List;
import java.util.Optional;

import com.myopty.order.domain.Order;
import com.myopty.order.domain.OrderStatus;

import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC access to the {@code progressive_order} table, owned by the
 * Order &amp; Prescription module.
 */
public interface OrderRepository extends CrudRepository<Order, Long> {

	/**
	 * Finds the order built from a prescription. Derived from the method name, so
	 * the property has to keep being called {@code prescriptionId}.
	 *
	 * <p>At most one row can match because {@code prescription_id} carries a unique
	 * key, which is what makes the one-to-one link a fact rather than a
	 * convention: two orders for one prescription cannot exist even if the
	 * application check that guards this is bypassed.
	 */
	Optional<Order> findByPrescriptionId(Long prescriptionId);

	/**
	 * The client's approval queue: the orders waiting on a decision, oldest first.
	 *
	 * <p>Derived from the method name, so the status property has to keep being
	 * called {@code status}. Ascending {@code createdAt} is deliberate: the shop
	 * works the queue in the order it arrived, which is the order the module
	 * documents for the order-processing story. {@code idx_progressive_order_status}
	 * from V8 serves the filter, and {@code OrderId} breaks ties within a timestamp
	 * so the order is stable across calls. The tie-break names the property
	 * {@code orderId} rather than {@code Id}, because the entity has no property
	 * called {@code id} and a derived query naming one fails to resolve.
	 */
	List<Order> findTop100ByStatusOrderByCreatedAtAscOrderIdAsc(OrderStatus status);

}
