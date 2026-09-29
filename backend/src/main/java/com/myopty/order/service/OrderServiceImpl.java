package com.myopty.order.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

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

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link OrderService}.
 *
 * <p>An order is meaningless without the prescription it is built from, so
 * creating one starts by reading that prescription: the order type the customer
 * picked has to agree with what the prescription can actually be made into, and
 * the order can never end up owned by somebody other than the prescription's
 * owner. Both rules need two rows in hand, which is why they live here rather
 * than in bean validation on the request.
 */
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

	private final OrderRepository repository;

	private final PrescriptionRepository prescriptions;

	public OrderServiceImpl(OrderRepository repository, PrescriptionRepository prescriptions) {
		this.repository = repository;
		this.prescriptions = prescriptions;
	}

	@Override
	public Order create(OrderCreateRequest request) {
		Prescription prescription = this.prescriptions.findById(request.prescriptionId())
			.orElseThrow(() -> new PrescriptionNotFoundException(request.prescriptionId()));

		validate(request, prescription);

		if (this.repository.findByPrescriptionId(prescription.getPrescriptionId()).isPresent()) {
			throw new OrderAlreadyExistsException(prescription.getPrescriptionId());
		}

		try {
			return this.repository.save(toOrder(request, prescription));
		}
		catch (DuplicateKeyException ex) {
			// The unique key on prescription_id is the real guard against two
			// orders for one prescription. The lookup above only spares the
			// customer a constraint violation when they are not racing anyone.
			throw new OrderAlreadyExistsException(prescription.getPrescriptionId());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Order getById(Long orderId) {
		return this.repository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
	}

	@Override
	@Transactional(readOnly = true)
	public Order getByPrescriptionId(Long prescriptionId) {
		return this.repository.findByPrescriptionId(prescriptionId)
			.orElseThrow(() -> new OrderNotFoundException(
					"Prescription " + prescriptionId + " has not been ordered yet"));
	}

	/**
	 * Enforces the rules that need the prescription as well as the request. Every
	 * problem is collected rather than reported one at a time, so a customer fixing
	 * a rejected order sees everything that is wrong with it in one go.
	 */
	private static void validate(OrderCreateRequest request, Prescription prescription) {
		Map<String, String> errors = new LinkedHashMap<>();
		checkOrderType(request, prescription, errors);
		checkCustomer(request, prescription, errors);
		if (!errors.isEmpty()) {
			throw new InvalidOrderException("Some order values are not valid", errors);
		}
	}

	private static void checkOrderType(OrderCreateRequest request, Prescription prescription,
			Map<String, String> errors) {
		OrderType orderType = request.orderType();
		if (orderType != null && orderType.requiresNearAddition() && !prescription.isProgressive()) {
			errors.put("orderType",
					"a " + orderType.label() + " needs a prescription with a near addition on both eyes");
		}
	}

	/**
	 * An order inherits the owner of its prescription. A customer may send the id
	 * as well, but only to agree with what is already there: a mismatch means the
	 * customer is trying to order against somebody else's prescription, and
	 * quietly overwriting the owner would hide that.
	 *
	 * <p>A prescription with no owner yet is adopted rather than rejected, because
	 * the customer table belongs to the shared module and is not written yet; the
	 * order inherits the id the customer supplied so the link is still there when
	 * that table lands.
	 */
	private static void checkCustomer(OrderCreateRequest request, Prescription prescription,
			Map<String, String> errors) {
		Long requested = request.customerId();
		if (requested == null || prescription.getCustomerId() == null) {
			return;
		}
		if (!requested.equals(prescription.getCustomerId())) {
			errors.put("customerId", "must match the customer the prescription belongs to");
		}
	}

	private static Order toOrder(OrderCreateRequest request, Prescription prescription) {
		Instant now = Instant.now();

		Order order = new Order();
		order.setCustomerId(prescription.getCustomerId() != null ? prescription.getCustomerId()
				: request.customerId());
		order.setPrescriptionId(prescription.getPrescriptionId());
		order.setOrderType(request.orderType());
		order.setFrameId(request.frameId());
		order.setLensId(request.lensId());
		order.setStatus(OrderStatus.PENDING_REVIEW);
		order.setCreatedAt(now);
		order.setUpdatedAt(now);
		return order;
	}

}
