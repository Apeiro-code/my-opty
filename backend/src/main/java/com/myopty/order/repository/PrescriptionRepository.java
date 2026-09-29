package com.myopty.order.repository;

import com.myopty.order.domain.Prescription;

import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC access to the {@code prescription} table, owned by the
 * Order &amp; Prescription module.
 */
public interface PrescriptionRepository extends CrudRepository<Prescription, Long> {

}
