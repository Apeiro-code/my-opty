package com.myopty.order.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.myopty.order.domain.Order;
import com.myopty.order.domain.Prescription;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * The queue queries are derived from their method names, and Spring Data only
 * checks those names when the application context starts. The unit tests mock the
 * repositories, so nothing else would stand between a typo in a method name and a
 * query that cannot be built; these tests parse the names instead and resolve
 * every property they mention against the entity.
 *
 * <p>That matters more than usual for these two, because the tie-break names the
 * id property and the two aggregates spell it differently:
 * {@code orderId} against {@code prescriptionId}. A sort clause that resolved for
 * one would not for the other, and a mistake in either would only surface at
 * startup.
 */
class QueueQueryTest {

	private static final String ORDER_QUEUE = "findTop100ByStatusOrderByCreatedAtAscOrderIdAsc";

	private static final String PRESCRIPTION_QUEUE = "findTop100ByStatusOrderByCreatedAtAscPrescriptionIdAsc";

	@Test
	void parsesTheOrderQueueQuery() {
		PartTree tree = orderQueue();

		assertThat(tree.hasPredicate()).isTrue();
		assertThat(tree.getMaxResults()).isEqualTo(100);
	}

	@Test
	void parsesThePrescriptionQueueQuery() {
		PartTree tree = prescriptionQueue();

		assertThat(tree.hasPredicate()).isTrue();
		assertThat(tree.getMaxResults()).isEqualTo(100);
	}

	/**
	 * The predicate has to be the status. A query that sorted without filtering
	 * would return the whole table under an endpoint that documents itself as
	 * filtered, and it would look correct in testing.
	 */
	@Test
	void filtersBothQueuesOnStatusAlone() {
		assertThat(predicateProperties(orderQueue())).containsExactly("status");
		assertThat(predicateProperties(prescriptionQueue())).containsExactly("status");
	}

	/**
	 * Oldest first, because the shop works a queue in the order it arrived, and the
	 * id breaks ties so two prescriptions uploaded in the same second cannot swap
	 * places between two identical calls.
	 */
	@Test
	void sortsOldestFirstWithAStableTieBreak() {
		assertThat(orderQueue().getSort())
			.isEqualTo(Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("orderId")));
		assertThat(prescriptionQueue().getSort())
			.isEqualTo(Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("prescriptionId")));
	}

	/**
	 * Every property either name mentions has to exist on the entity. This is the
	 * assertion that would otherwise only happen when the application started.
	 */
	@Test
	void everyPropertyNamedByTheQueueQueriesExists() {
		assertThatCode(this::parseBoth).doesNotThrowAnyException();
	}

	/**
	 * The queue names are checked in one place, so a rename that resolves but no
	 * longer means what it says is caught rather than left for a reader to spot.
	 */
	@Test
	void everyDeclaredRepositoryMethodResolvesItsProperties() throws Exception {
		for (Class<?> repository : List.of(OrderRepository.class, PrescriptionRepository.class)) {
			for (Method method : List.of(repository.getDeclaredMethods())) {
				assertThatCode(() -> new PartTree(method.getName(), entityOf(repository)).getSort())
					.as("%s#%s", repository.getSimpleName(), method.getName())
					.doesNotThrowAnyException();
			}
		}
	}

	/**
	 * Only the queue methods promise a cap. {@code findByPrescriptionId} is a
	 * reverse lookup of a unique key and returns at most one row whatever the
	 * database is asked for, so it deliberately has no limit in its name.
	 */
	@Test
	void onlyTheQueueQueriesAreCapped() {
		assertThat(orderQueue().getMaxResults()).isEqualTo(100);
		assertThat(prescriptionQueue().getMaxResults()).isEqualTo(100);
		assertThat(new PartTree("findByPrescriptionId", Order.class).getMaxResults()).isNull();
	}

	private void parseBoth() {
		new PartTree(ORDER_QUEUE, Order.class).getSort();
		new PartTree(PRESCRIPTION_QUEUE, Prescription.class).getSort();
	}

	private PartTree orderQueue() {
		return new PartTree(ORDER_QUEUE, Order.class);
	}

	private PartTree prescriptionQueue() {
		return new PartTree(PRESCRIPTION_QUEUE, Prescription.class);
	}

	private static List<String> predicateProperties(PartTree tree) {
		return tree.getParts(Part.Type.SIMPLE_PROPERTY)
			.stream()
			.map(part -> part.getProperty().getSegment())
			.toList();
	}

	private static Class<?> entityOf(Class<?> repository) {
		return repository == OrderRepository.class ? Order.class : Prescription.class;
	}

}
