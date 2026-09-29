package com.myopty.order.domain;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A customer's order for a frame and lens built to one of their prescriptions.
 *
 * <p>The order is the aggregate root: it holds the id of the prescription it is
 * built from rather than the other way round, so creating an order and reading it
 * back never has to write to two aggregates in one transaction. The link is
 * one-to-one, as the module EER diagram requires, and {@code prescription_id}
 * carries a unique key in {@code progressive_order} so the database refuses a
 * second order for the same prescription even if two requests race.
 *
 * <p>{@code frameId} and {@code lensId} are plain columns with no foreign key:
 * the catalog tables that would own them do not exist yet.
 */
@Table("progressive_order")
public class Order {

	@Id
	private Long orderId;

	private Long customerId;

	private Long prescriptionId;

	private Long frameId;

	private Long lensId;

	private OrderType orderType = OrderType.SINGLE_VISION;

	private OrderStatus status = OrderStatus.PENDING_REVIEW;

	/**
	 * Why the shop rejected this order, set only alongside
	 * {@link OrderStatus#REJECTED}.
	 *
	 * <p>Carried on the order rather than read back off the prescription because
	 * the two decisions are independent: an order can be turned down for a stock
	 * or pricing reason while its prescription stays verified.
	 */
	private String rejectionReason;

	private LocalDate receiveDate;

	private Instant createdAt;

	private Instant updatedAt;

	public Order() {
	}

	/**
	 * @return true once the shop has quoted a date for this order. Null until then,
	 *         which is why the column is nullable rather than defaulted.
	 */
	public boolean hasReceiveDate() {
		return this.receiveDate != null;
	}

	/**
	 * @return true once the shop has turned this order down and said why. Null on
	 *         every order that is progressing normally, which is why the column is
	 *         nullable rather than carrying an empty string.
	 */
	public boolean hasRejectionReason() {
		return this.rejectionReason != null;
	}

	public Long getOrderId() {
		return this.orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public Long getCustomerId() {
		return this.customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}

	public Long getPrescriptionId() {
		return this.prescriptionId;
	}

	public void setPrescriptionId(Long prescriptionId) {
		this.prescriptionId = prescriptionId;
	}

	public Long getFrameId() {
		return this.frameId;
	}

	public void setFrameId(Long frameId) {
		this.frameId = frameId;
	}

	public Long getLensId() {
		return this.lensId;
	}

	public void setLensId(Long lensId) {
		this.lensId = lensId;
	}

	public OrderType getOrderType() {
		return this.orderType;
	}

	public void setOrderType(OrderType orderType) {
		this.orderType = orderType;
	}

	public OrderStatus getStatus() {
		return this.status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public String getRejectionReason() {
		return this.rejectionReason;
	}

	public void setRejectionReason(String rejectionReason) {
		this.rejectionReason = rejectionReason;
	}

	public LocalDate getReceiveDate() {
		return this.receiveDate;
	}

	public void setReceiveDate(LocalDate receiveDate) {
		this.receiveDate = receiveDate;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

}
