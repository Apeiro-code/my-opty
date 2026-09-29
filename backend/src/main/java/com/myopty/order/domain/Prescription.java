package com.myopty.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A customer's prescription for a custom lens.
 *
 * <p>Optical values are stored per eye and flat, mirroring the {@code prescription}
 * table and the module EER diagram ({@code sph_left}, {@code sph_right}, {@code cyl}, {@code axis}).
 * The uploaded document contributes metadata columns only: the bytes live in object
 * storage and are reached through {@link #getDocumentObjectKey()}.
 */
@Table("prescription")
public class Prescription {

	@Id
	private Long prescriptionId;

	private Long customerId;

	/**
	 * Named explicitly: the column follows the module EER diagram
	 * ({@code is_progressive}), which the default camelCase strategy would not
	 * produce from the property name.
	 */
	@Column("is_progressive")
	private boolean progressive;

	private LocalDate issuedDate;

	private BigDecimal sphRight;

	private BigDecimal cylRight;

	private Integer axisRight;

	private BigDecimal addRight;

	private BigDecimal sphLeft;

	private BigDecimal cylLeft;

	private Integer axisLeft;

	private BigDecimal addLeft;

	private String notes;

	private PrescriptionStatus status = PrescriptionStatus.PENDING_REVIEW;

	private String rejectionReason;

	private String documentObjectKey;

	private String documentFilename;

	private String documentContentType;

	private Long documentSizeBytes;

	private Instant documentUploadedAt;

	private Instant createdAt;

	private Instant updatedAt;

	public Prescription() {
	}

	public EyePrescription rightEye() {
		return new EyePrescription(this.sphRight, this.cylRight, this.axisRight, this.addRight);
	}

	public EyePrescription leftEye() {
		return new EyePrescription(this.sphLeft, this.cylLeft, this.axisLeft, this.addLeft);
	}

	public boolean hasDocument() {
		return this.documentObjectKey != null && !this.documentObjectKey.isBlank();
	}

	public PrescriptionDocument document() {
		return this.hasDocument() ? new PrescriptionDocument(this.documentObjectKey, this.documentFilename,
				this.documentContentType, this.documentSizeBytes, this.documentUploadedAt) : null;
	}

	public Long getPrescriptionId() {
		return this.prescriptionId;
	}

	public void setPrescriptionId(Long prescriptionId) {
		this.prescriptionId = prescriptionId;
	}

	public Long getCustomerId() {
		return this.customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}

	public boolean isProgressive() {
		return this.progressive;
	}

	public void setProgressive(boolean progressive) {
		this.progressive = progressive;
	}

	public LocalDate getIssuedDate() {
		return this.issuedDate;
	}

	public void setIssuedDate(LocalDate issuedDate) {
		this.issuedDate = issuedDate;
	}

	public BigDecimal getSphRight() {
		return this.sphRight;
	}

	public void setSphRight(BigDecimal sphRight) {
		this.sphRight = sphRight;
	}

	public BigDecimal getCylRight() {
		return this.cylRight;
	}

	public void setCylRight(BigDecimal cylRight) {
		this.cylRight = cylRight;
	}

	public Integer getAxisRight() {
		return this.axisRight;
	}

	public void setAxisRight(Integer axisRight) {
		this.axisRight = axisRight;
	}

	public BigDecimal getAddRight() {
		return this.addRight;
	}

	public void setAddRight(BigDecimal addRight) {
		this.addRight = addRight;
	}

	public BigDecimal getSphLeft() {
		return this.sphLeft;
	}

	public void setSphLeft(BigDecimal sphLeft) {
		this.sphLeft = sphLeft;
	}

	public BigDecimal getCylLeft() {
		return this.cylLeft;
	}

	public void setCylLeft(BigDecimal cylLeft) {
		this.cylLeft = cylLeft;
	}

	public Integer getAxisLeft() {
		return this.axisLeft;
	}

	public void setAxisLeft(Integer axisLeft) {
		this.axisLeft = axisLeft;
	}

	public BigDecimal getAddLeft() {
		return this.addLeft;
	}

	public void setAddLeft(BigDecimal addLeft) {
		this.addLeft = addLeft;
	}

	public String getNotes() {
		return this.notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public PrescriptionStatus getStatus() {
		return this.status;
	}

	public void setStatus(PrescriptionStatus status) {
		this.status = status;
	}

	public String getRejectionReason() {
		return this.rejectionReason;
	}

	public void setRejectionReason(String rejectionReason) {
		this.rejectionReason = rejectionReason;
	}

	public String getDocumentObjectKey() {
		return this.documentObjectKey;
	}

	public void setDocumentObjectKey(String documentObjectKey) {
		this.documentObjectKey = documentObjectKey;
	}

	public String getDocumentFilename() {
		return this.documentFilename;
	}

	public void setDocumentFilename(String documentFilename) {
		this.documentFilename = documentFilename;
	}

	public String getDocumentContentType() {
		return this.documentContentType;
	}

	public void setDocumentContentType(String documentContentType) {
		this.documentContentType = documentContentType;
	}

	public Long getDocumentSizeBytes() {
		return this.documentSizeBytes;
	}

	public void setDocumentSizeBytes(Long documentSizeBytes) {
		this.documentSizeBytes = documentSizeBytes;
	}

	public Instant getDocumentUploadedAt() {
		return this.documentUploadedAt;
	}

	public void setDocumentUploadedAt(Instant documentUploadedAt) {
		this.documentUploadedAt = documentUploadedAt;
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
