package com.myopty.order.repository;

import java.util.Optional;

import com.myopty.order.domain.Order;

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

}
