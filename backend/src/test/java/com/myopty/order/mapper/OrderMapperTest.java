package com.myopty.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import com.myopty.order.domain.Order;
import com.myopty.order.domain.OrderStatus;
import com.myopty.order.domain.OrderType;
import com.myopty.order.dto.OrderResponse;

import org.junit.jupiter.api.Test;

/**
 * The shape an order takes on the wire. The enums are the part worth pinning
 * down: a client that receives {@code "PROGRESSIVE"} can send it back unchanged,
 * while a JSON object per value would have to be kept in step with the enum.
 */
class OrderMapperTest {

	@Test
	void carriesTheEnumsAsTheirNames() {
		OrderResponse response = OrderMapper.toResponse(order());

		assertThat(response.orderType()).isEqualTo("PROGRESSIVE");
		assertThat(response.status()).isEqualTo("PENDING_REVIEW");
	}

	@Test
	void carriesTheLinkToThePrescription() {
		OrderResponse response = OrderMapper.toResponse(order());

		assertThat(response.prescriptionId()).isEqualTo(12L);
		assertThat(response.customerId()).isEqualTo(7L);
	}

	@Test
	void carriesTheFrameAndLensWhenTheyAreKnown() {
		OrderResponse response = OrderMapper.toResponse(order());

		assertThat(response.frameId()).isEqualTo(4L);
		assertThat(response.lensId()).isEqualTo(9L);
	}

	@Test
	void leavesTheFrameAndLensOutWhenTheCatalogDidNotSupplyThem() {
		Order order = order();
		order.setFrameId(null);
		order.setLensId(null);

		OrderResponse response = OrderMapper.toResponse(order);

		assertThat(response.frameId()).isNull();
		assertThat(response.lensId()).isNull();
	}

	/**
	 * A row read back before it was written can still have null enums. Mapping
	 * must not turn that into a 500 on the way out of a read.
	 */
	@Test
	void toleratesAnOrderWhoseEnumsHaveNotBeenSet() {
		Order order = order();
		order.setOrderType(null);
		order.setStatus(null);

		OrderResponse response = OrderMapper.toResponse(order);

		assertThat(response.orderType()).isNull();
		assertThat(response.status()).isNull();
	}

	@Test
	void carriesTheReceiveDateAndTimestamps() {
		OrderResponse response = OrderMapper.toResponse(order());

		assertThat(response.receiveDate()).isEqualTo(LocalDate.of(2026, 10, 20));
		assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-09-29T10:15:30Z"));
		assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-09-29T10:15:30Z"));
	}

	@Test
	void carriesTheRejectionReasonOnceTheOrderIsRejected() {
		Order order = order();
		order.setStatus(OrderStatus.REJECTED);
		order.setRejectionReason("frame out of stock");

		OrderResponse response = OrderMapper.toResponse(order);

		assertThat(response.status()).isEqualTo("REJECTED");
		assertThat(response.rejectionReason()).isEqualTo("frame out of stock");
	}

	/**
	 * Left null rather than empty so the field is omitted from the JSON entirely,
	 * and a client cannot mistake an absent reason for a reason of "".
	 */
	@Test
	void leavesTheRejectionReasonOutUntilThereIsOne() {
		assertThat(OrderMapper.toResponse(order()).rejectionReason()).isNull();
	}

	private static Order order() {
		Order order = new Order();
		order.setOrderId(3L);
		order.setCustomerId(7L);
		order.setPrescriptionId(12L);
		order.setFrameId(4L);
		order.setLensId(9L);
		order.setOrderType(OrderType.PROGRESSIVE);
		order.setStatus(OrderStatus.PENDING_REVIEW);
		order.setReceiveDate(LocalDate.of(2026, 10, 20));
		order.setCreatedAt(Instant.parse("2026-09-29T10:15:30Z"));
		order.setUpdatedAt(Instant.parse("2026-09-29T10:15:30Z"));
		return order;
	}

}
