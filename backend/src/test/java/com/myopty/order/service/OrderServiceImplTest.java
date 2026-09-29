package com.myopty.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.myopty.order.domain.Order;
import com.myopty.order.domain.OrderStatus;
import com.myopty.order.domain.OrderType;
import com.myopty.order.domain.Prescription;
import com.myopty.order.dto.OrderCreateRequest;
import com.myopty.order.exception.InvalidOrderException;
import com.myopty.order.exception.OrderAlreadyExistsException;
import com.myopty.order.exception.OrderNotFoundException;
import com.myopty.order.exception.PrescriptionNotFoundException;
import com.myopty.order.repository.OrderRepository;
import com.myopty.order.repository.PrescriptionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/**
 * Order placement rules: the order type a customer may pick for a given
 * prescription, the one-to-one link to the prescription, and the ownership that
 * stops an order being placed against somebody else's prescription.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

	private static final Long PRESCRIPTION_ID = 12L;

	private static final Long CUSTOMER_ID = 7L;

	@Mock
	private OrderRepository repository;

	@Mock
	private PrescriptionRepository prescriptions;

	private OrderServiceImpl service;

	@BeforeEach
	void setUp() {
		this.service = new OrderServiceImpl(this.repository, this.prescriptions);
	}

	@Test
	void linksTheOrderToThePrescriptionAndQueuesReview() {
		stubPrescription(progressivePrescription());
		stubNoExistingOrder();
		stubSaveEchoingTheId();

		Order saved = this.service.create(request(OrderType.PROGRESSIVE));

		assertThat(saved.getOrderType()).isEqualTo(OrderType.PROGRESSIVE);
		assertThat(saved.getPrescriptionId()).isEqualTo(PRESCRIPTION_ID);
		assertThat(saved.getCustomerId()).isEqualTo(CUSTOMER_ID);
		assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING_REVIEW);
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	/**
	 * A customer never picks the workflow state, so nothing in the request can move
	 * an order past the shop's review.
	 */
	@Test
	void neverLetsTheOrderStartAnywhereButReview() {
		stubPrescription(progressivePrescription());
		stubNoExistingOrder();
		stubSaveEchoingTheId();

		assertThat(this.service.create(request(OrderType.SINGLE_VISION)).getStatus())
			.isEqualTo(OrderStatus.PENDING_REVIEW);
	}

	@Test
	void recordsTheFrameAndLensWhenTheCustomerSuppliesThem() {
		stubPrescription(progressivePrescription());
		stubNoExistingOrder();
		stubSaveEchoingTheId();

		Order saved = this.service.create(new OrderCreateRequest(null, PRESCRIPTION_ID, OrderType.PROGRESSIVE, 4L, 9L));

		assertThat(saved.getFrameId()).isEqualTo(4L);
		assertThat(saved.getLensId()).isEqualTo(9L);
	}

	/**
	 * The catalog tables do not exist yet, so the frame and lens are optional:
	 * requiring them would make the whole feature unusable until another owner
	 * lands their migration.
	 */
	@Test
	void placesAnOrderWithoutAFrameOrLensWhenTheCatalogIsNotReachable() {
		stubPrescription(progressivePrescription());
		stubNoExistingOrder();
		stubSaveEchoingTheId();

		Order saved = this.service.create(request(OrderType.PROGRESSIVE));

		assertThat(saved.getFrameId()).isNull();
		assertThat(saved.getLensId()).isNull();
	}

	@Test
	void acceptsAProgressiveOrderOnAPrescriptionWithANearAddition() {
		stubPrescription(progressivePrescription());
		stubNoExistingOrder();
		stubSaveEchoingTheId();

		assertThat(this.service.create(request(OrderType.PROGRESSIVE)).getOrderType())
			.isEqualTo(OrderType.PROGRESSIVE);
	}

	@Test
	void acceptsASingleVisionOrderOnAPrescriptionWithNoNearAddition() {
		stubPrescription(plainPrescription());
		stubNoExistingOrder();
		stubSaveEchoingTheId();

		assertThat(this.service.create(request(OrderType.SINGLE_VISION)).getOrderType())
			.isEqualTo(OrderType.SINGLE_VISION);
	}

	/**
	 * The rule that keeps the two stories consistent: the order type the customer
	 * selects has to be one the prescription can actually be made into, or the shop
	 * would queue a lens for the lab that has no near addition to cut.
	 */
	@Test
	void refusesAProgressiveOrderOnAPrescriptionWithNoNearAddition() {
		stubPrescription(plainPrescription());

		assertThatThrownBy(() -> this.service.create(request(OrderType.PROGRESSIVE)))
			.isInstanceOf(InvalidOrderException.class)
			.satisfies(ex -> assertThat(((InvalidOrderException) ex).getFieldErrors())
				.containsEntry("orderType", "a progressive lens needs a prescription with a near addition on both eyes"));
	}

	@Test
	void refusesABifocalOrderOnAPrescriptionWithNoNearAddition() {
		stubPrescription(plainPrescription());

		assertThatThrownBy(() -> this.service.create(request(OrderType.BIFOCAL))).isInstanceOf(InvalidOrderException.class)
			.satisfies(ex -> assertThat(((InvalidOrderException) ex).getFieldErrors()).containsKey("orderType"));
	}

	@Test
	void reportsEveryProblemWithAnOrderAtOnce() {
		stubPrescription(plainPrescription());

		assertThatThrownBy(
				() -> this.service.create(new OrderCreateRequest(99L, PRESCRIPTION_ID, OrderType.PROGRESSIVE, null, null)))
			.isInstanceOf(InvalidOrderException.class)
			.satisfies(ex -> assertThat(((InvalidOrderException) ex).getFieldErrors()).containsOnlyKeys("orderType",
					"customerId"));
	}

	@Test
	void refusesAnOrderAgainstSomebodyElsesPrescription() {
		stubPrescription(progressivePrescription());

		assertThatThrownBy(
				() -> this.service.create(new OrderCreateRequest(99L, PRESCRIPTION_ID, OrderType.PROGRESSIVE, null, null)))
			.isInstanceOf(InvalidOrderException.class)
			.satisfies(ex -> assertThat(((InvalidOrderException) ex).getFieldErrors())
				.containsEntry("customerId", "must match the customer the prescription belongs to"));
	}

	/**
	 * Until the shared user table lands a prescription has no owner, so refusing an
	 * order for that reason would block every customer. The order adopts the id the
	 * customer sent instead.
	 */
	@Test
	void adoptsTheCustomerWhenThePrescriptionDoesNotHaveOneYet() {
		Prescription unowned = plainPrescription();
		unowned.setCustomerId(null);
		stubPrescription(unowned);
		stubNoExistingOrder();
		stubSaveEchoingTheId();

		Order saved = this.service
			.create(new OrderCreateRequest(CUSTOMER_ID, PRESCRIPTION_ID, OrderType.SINGLE_VISION, null, null));

		assertThat(saved.getCustomerId()).isEqualTo(CUSTOMER_ID);
	}

	@Test
	void inheritsTheCustomerFromThePrescriptionWhenTheRequestOmitsIt() {
		stubPrescription(progressivePrescription());
		stubNoExistingOrder();
		stubSaveEchoingTheId();

		assertThat(this.service.create(request(OrderType.PROGRESSIVE)).getCustomerId()).isEqualTo(CUSTOMER_ID);
	}

	@Test
	void reportsAPrescriptionThatDoesNotExist() {
		when(this.prescriptions.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.create(new OrderCreateRequest(null, 404L, OrderType.SINGLE_VISION, null, null)))
			.isInstanceOf(PrescriptionNotFoundException.class)
			.hasMessage("Prescription 404 was not found");
	}

	/**
	 * The one-to-one link: a prescription that has already been ordered cannot be
	 * ordered again, or the shop would manufacture the same pair twice.
	 */
	@Test
	void refusesASecondOrderForTheSamePrescription() {
		stubPrescription(progressivePrescription());
		when(this.repository.findByPrescriptionId(PRESCRIPTION_ID)).thenReturn(Optional.of(new Order()));

		assertThatThrownBy(() -> this.service.create(request(OrderType.PROGRESSIVE)))
			.isInstanceOf(OrderAlreadyExistsException.class)
			.hasMessage("Prescription 12 has already been ordered");
	}

	/**
	 * The lookup above only spares the customer a constraint violation when nobody
	 * else is ordering at the same moment. When they are, the unique key is what
	 * rejects the insert, and the customer gets the same answer either way.
	 */
	@Test
	void reportsTheSameConflictWhenTwoOrdersRacePastTheCheck() {
		stubPrescription(progressivePrescription());
		stubNoExistingOrder();
		when(this.repository.save(any(Order.class)))
			.thenThrow(new DuplicateKeyException("uk_progressive_order_prescription"));

		assertThatThrownBy(() -> this.service.create(request(OrderType.PROGRESSIVE)))
			.isInstanceOf(OrderAlreadyExistsException.class)
			.hasMessage("Prescription 12 has already been ordered");
	}

	@Test
	void writesNothingWhenTheOrderTypeDoesNotFit() {
		stubPrescription(plainPrescription());

		assertThatThrownBy(() -> this.service.create(request(OrderType.PROGRESSIVE)))
			.isInstanceOf(InvalidOrderException.class);

		verify(this.repository, never()).save(any(Order.class));
	}

	@Test
	void returnsAnOrderByItsId() {
		Order stored = new Order();
		stored.setOrderId(3L);
		when(this.repository.findById(3L)).thenReturn(Optional.of(stored));

		assertThat(this.service.getById(3L)).isSameAs(stored);
	}

	@Test
	void reportsAnUnknownOrderId() {
		when(this.repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.getById(99L)).isInstanceOf(OrderNotFoundException.class)
			.hasMessage("Order 99 was not found");
	}

	/**
	 * The other direction of the link, and the reason the repository has a reverse
	 * lookup at all: a customer holding a prescription needs to find the order made
	 * from it without knowing the order id.
	 */
	@Test
	void findsTheOrderBuiltFromAPrescription() {
		Order stored = new Order();
		stored.setOrderId(3L);
		stored.setPrescriptionId(PRESCRIPTION_ID);
		when(this.repository.findByPrescriptionId(PRESCRIPTION_ID)).thenReturn(Optional.of(stored));

		assertThat(this.service.getByPrescriptionId(PRESCRIPTION_ID)).isSameAs(stored);
	}

	@Test
	void reportsAPrescriptionThatHasNotBeenOrdered() {
		when(this.repository.findByPrescriptionId(PRESCRIPTION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.getByPrescriptionId(PRESCRIPTION_ID))
			.isInstanceOf(OrderNotFoundException.class)
			.hasMessage("Prescription 12 has not been ordered yet");
	}

	/**
	 * Nothing in the order module may change the prescription it is built from, or
	 * the order would silently re-point itself at different optical values.
	 */
	@Test
	void leavesThePrescriptionUntouched() {
		stubPrescription(progressivePrescription());
		stubNoExistingOrder();
		stubSaveEchoingTheId();

		this.service.create(request(OrderType.PROGRESSIVE));

		verify(this.prescriptions, never()).save(any(Prescription.class));
	}

	private void stubPrescription(Prescription prescription) {
		when(this.prescriptions.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription));
	}

	private void stubNoExistingOrder() {
		when(this.repository.findByPrescriptionId(PRESCRIPTION_ID)).thenReturn(Optional.empty());
	}

	/**
	 * Echoes the saved order back the way Spring Data JDBC does, assigning the
	 * generated key, so the test can assert on what was written.
	 */
	private void stubSaveEchoingTheId() {
		when(this.repository.save(any(Order.class))).thenAnswer(invocation -> {
			Order order = invocation.getArgument(0);
			order.setOrderId(3L);
			return order;
		});
	}

	private static OrderCreateRequest request(OrderType orderType) {
		return new OrderCreateRequest(CUSTOMER_ID, PRESCRIPTION_ID, orderType, null, null);
	}

	private static Prescription plainPrescription() {
		Prescription prescription = new Prescription();
		prescription.setPrescriptionId(PRESCRIPTION_ID);
		prescription.setCustomerId(CUSTOMER_ID);
		prescription.setProgressive(false);
		return prescription;
	}

	private static Prescription progressivePrescription() {
		Prescription prescription = plainPrescription();
		prescription.setProgressive(true);
		return prescription;
	}

}
