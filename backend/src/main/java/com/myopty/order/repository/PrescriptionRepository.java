package com.myopty.order.repository;

import java.util.List;

import com.myopty.order.domain.Prescription;
import com.myopty.order.domain.PrescriptionStatus;

import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC access to the {@code prescription} table, owned by the
 * Order &amp; Prescription module.
 */
public interface PrescriptionRepository extends CrudRepository<Prescription, Long> {

	/**
	 * The client's review queue: the prescriptions waiting on a decision, oldest
	 * first.
	 *
	 * <p>Derived from the method name, so the status property has to keep being
	 * called {@code status}. Ascending {@code createdAt} is deliberate: the shop
	 * reviews in the order prescriptions arrived. The {@code Top100} is the
	 * cap the queue endpoint promises, and putting it in the query rather than in
	 * a {@code subList} means the database stops sending rows the client would
	 * only throw away. {@code prescription_id} breaks ties within a timestamp so
	 * the queue does not reshuffle between two identical calls.
	 */
	List<Prescription> findTop100ByStatusOrderByCreatedAtAscPrescriptionIdAsc(PrescriptionStatus status);

}
