package com.myopty.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.myopty.order.domain.Prescription;
import com.myopty.order.domain.PrescriptionDocument;
import com.myopty.order.domain.PrescriptionStatus;
import com.myopty.order.dto.EyeRequest;
import com.myopty.order.dto.PrescriptionCreateRequest;
import com.myopty.order.exception.PrescriptionDocumentNotFoundException;
import com.myopty.order.exception.DocumentStorageException;
import com.myopty.order.exception.InvalidPrescriptionDocumentException;
import com.myopty.order.exception.InvalidPrescriptionException;
import com.myopty.order.exception.PrescriptionNotFoundException;
import com.myopty.order.repository.PrescriptionRepository;
import com.myopty.order.service.storage.DocumentStorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Prescription submission rules: what the customer may submit, what reaches object
 * storage, and what is cleaned up when the database write fails.
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionServiceImplTest {

	@Mock
	private PrescriptionRepository repository;

	@Mock
	private DocumentStorage storage;

	private PrescriptionServiceImpl service;

	@BeforeEach
	void setUp() {
		this.service = new PrescriptionServiceImpl(this.repository, this.storage,
				new DocumentValidator(TestDocuments.properties()));
	}

	@Test
	void storesTypedValuesWithTheDocumentAndQueuesReview() {
		MockMultipartFile document = jpeg("rx-scan.jpg");
		stubStorageSuccess();

		Prescription saved = this.service.create(request(eye("-2.00", "-0.75", "180", "2.00"), eye("-2.25", null, null,
				"2.00"), true, LocalDate.of(2026, 3, 14)), document);

		assertThat(saved.getStatus()).isEqualTo(PrescriptionStatus.PENDING_REVIEW);
		assertThat(saved.getSphRight()).isEqualByComparingTo("-2.00");
		assertThat(saved.getCylRight()).isEqualByComparingTo("-0.75");
		assertThat(saved.getAxisRight()).isEqualTo(180);
		assertThat(saved.getAddRight()).isEqualByComparingTo("2.00");
		assertThat(saved.getSphLeft()).isEqualByComparingTo("-2.25");
		assertThat(saved.getIssuedDate()).isEqualTo(LocalDate.of(2026, 3, 14));
		assertThat(saved.isProgressive()).isTrue();
		assertThat(saved.getDocumentFilename()).isEqualTo("rx-scan.jpg");
		assertThat(saved.getDocumentContentType()).isEqualTo("image/jpeg");
		assertThat(saved.getDocumentSizeBytes()).isEqualTo(TestDocuments.JPEG_BYTES.length);
		assertThat(saved.getDocumentUploadedAt()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
	}

	@Test
	void keysTheDocumentByUploadMonthAndRandomName() {
		stubStorageSuccess();

		this.service.create(request(eye("-1.00", null, null, null), eye("-1.00", null, null, null), false, null),
				jpeg("rx-scan.jpg"));

		ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
		verify(this.storage).store(key.capture(), any(), anyLong(), anyString(), eq("image/jpeg"));
		assertThat(key.getValue()).startsWith("prescriptions/").endsWith(".jpg");
	}

	/**
	 * The stored extension follows the detected media type, never the uploaded
	 * filename, so a renamed file cannot control the object name.
	 */
	@Test
	void derivesTheStoredExtensionFromTheFileContent() {
		stubStorageSuccess();
		MockMultipartFile document = new MockMultipartFile("document", "sneaky.jpg", "application/pdf",
				TestDocuments.PDF_BYTES);

		this.service.create(request(eye("-1.00", null, null, null), eye("-1.00", null, null, null), false, null),
				document);

		ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
		verify(this.storage).store(key.capture(), any(), anyLong(), anyString(), eq("application/pdf"));
		assertThat(key.getValue()).endsWith(".pdf");
	}

	@Test
	void stripsAnyPathFromTheStoredFilename() {
		stubStorageSuccess();
		MockMultipartFile document = new MockMultipartFile("document", "../../etc/passwd.jpg", "image/jpeg",
				TestDocuments.JPEG_BYTES);

		Prescription saved = this.service.create(
				request(eye("-1.00", null, null, null), eye("-1.00", null, null, null), false, null), document);

		assertThat(saved.getDocumentFilename()).isEqualTo("passwd.jpg");
	}

	@Test
	void treatsZeroAddPowerAsNoAddition() {
		stubStorageSuccess();

		Prescription saved = this.service.create(request(eye("-1.00", null, null, "0.00"), eye("-1.00", null, null,
				"0.00"), false, null), jpeg("rx.jpg"));

		assertThat(saved.getAddRight()).isNull();
		assertThat(saved.getAddLeft()).isNull();
	}

	@Test
	void requiresAddPowerOnBothEyesForAProgressiveLens() {
		PrescriptionCreateRequest request = request(eye("-1.00", null, null, "2.00"), eye("-1.25", null, null, null),
				true, null);

		assertThatThrownBy(() -> this.service.create(request, jpeg("rx.jpg")))
			.isInstanceOf(InvalidPrescriptionException.class)
			.satisfies(ex -> assertThat(((InvalidPrescriptionException) ex).getFieldErrors())
				.containsOnlyKeys("leftEye.addPower"));
		verify(this.storage, never()).store(anyString(), any(), anyLong(), anyString(), anyString());
	}

	@Test
	void requiresAnAxisWhenTheCylinderIsNotZero() {
		PrescriptionCreateRequest request = request(eye("-1.00", "-0.50", null, null), eye("-1.00", null, null, null),
				false, null);

		assertThatThrownBy(() -> this.service.create(request, jpeg("rx.jpg")))
			.isInstanceOf(InvalidPrescriptionException.class)
			.satisfies(ex -> assertThat(((InvalidPrescriptionException) ex).getFieldErrors())
				.containsEntry("rightEye.axis", "is required when the cylinder is not 0.00"));
	}

	@Test
	void rejectsAnAxisWithoutACylinder() {
		PrescriptionCreateRequest request = request(eye("-1.00", "0.00", "90", null), eye("-1.00", null, null, null),
				false, null);

		assertThatThrownBy(() -> this.service.create(request, jpeg("rx.jpg")))
			.isInstanceOf(InvalidPrescriptionException.class)
			.satisfies(ex -> assertThat(((InvalidPrescriptionException) ex).getFieldErrors())
				.containsEntry("rightEye.axis", "must be 0 or blank when the cylinder is 0.00"));
	}

	@Test
	void rejectsValuesThatAreNotInQuarterDioptreSteps() {
		PrescriptionCreateRequest request = request(eye("-2.10", null, null, null), eye("-1.00", null, null, null), false,
				null);

		assertThatThrownBy(() -> this.service.create(request, jpeg("rx.jpg")))
			.isInstanceOf(InvalidPrescriptionException.class)
			.satisfies(ex -> assertThat(((InvalidPrescriptionException) ex).getFieldErrors())
				.containsEntry("rightEye.sphere", "must be in 0.25 dioptre steps"));
	}

	@Test
	void rejectsValuesOutsideTheClinicalRange() {
		PrescriptionCreateRequest request = request(eye("-32.00", null, null, null), eye("-1.00", null, null, null), false,
				null);

		assertThatThrownBy(() -> this.service.create(request, jpeg("rx.jpg")))
			.isInstanceOf(InvalidPrescriptionException.class)
			.satisfies(ex -> assertThat(((InvalidPrescriptionException) ex).getFieldErrors())
				.containsEntry("rightEye.sphere", "must be between -30.00 and 30.00"));
	}

	@Test
	void rejectsAnAxisOutsideTheDegreeRange() {
		PrescriptionCreateRequest request = request(eye("-1.00", "-0.50", "190", null), eye("-1.00", null, null, null),
				false, null);

		assertThatThrownBy(() -> this.service.create(request, jpeg("rx.jpg")))
			.isInstanceOf(InvalidPrescriptionException.class)
			.satisfies(ex -> assertThat(((InvalidPrescriptionException) ex).getFieldErrors())
				.containsKey("rightEye.axis"));
	}

	@Test
	void rejectsASubmissionWithoutADocument() {
		PrescriptionCreateRequest request = request(eye("-1.00", null, null, null), eye("-1.00", null, null, null), false,
				null);

		assertThatThrownBy(() -> this.service.create(request, new MockMultipartFile("document", "", "image/jpeg",
				new byte[0]))).isInstanceOf(InvalidPrescriptionDocumentException.class);
		verify(this.storage, never()).store(anyString(), any(), anyLong(), anyString(), anyString());
	}

	/**
	 * Nothing may be left in the bucket when the row never lands, otherwise retries
	 * would pile up documents the shop can never see.
	 */
	@Test
	void removesTheStoredDocumentWhenTheWriteFails() {
		stubStorageOnly();
		when(this.repository.save(any(Prescription.class))).thenThrow(new IllegalStateException("db down"));

		PrescriptionCreateRequest request = request(eye("-1.00", null, null, null), eye("-1.00", null, null, null), false,
				null);

		assertThatThrownBy(() -> this.service.create(request, jpeg("rx.jpg"))).isInstanceOf(IllegalStateException.class);
		verify(this.storage).delete(anyString());
	}
	@Test
	void reportsAMissingPrescription() {
		when(this.repository.findById(42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.getById(42L)).isInstanceOf(PrescriptionNotFoundException.class)
			.hasMessageContaining("42");
	}

	@Test
	void streamsTheStoredDocument() {
		Prescription prescription = new Prescription();
		prescription.setPrescriptionId(7L);
		prescription.setDocumentObjectKey("prescriptions/2026/09/abc.jpg");
		prescription.setDocumentFilename("rx.jpg");
		when(this.repository.findById(7L)).thenReturn(Optional.of(prescription));
		InputStream bytes = new ByteArrayInputStream(TestDocuments.JPEG_BYTES);
		when(this.storage.open("prescriptions/2026/09/abc.jpg"))
			.thenReturn(new DocumentStorage.DocumentContent(bytes, "image/jpeg", TestDocuments.JPEG_BYTES.length));

		var content = this.service.getDocument(7L);

		assertThat(content.filename()).isEqualTo("rx.jpg");
		assertThat(content.contentType()).isEqualTo("image/jpeg");
		assertThat(content.sizeBytes()).isEqualTo(TestDocuments.JPEG_BYTES.length);
		assertThat(content.stream()).isSameAs(bytes);
	}

	@Test
	void reportsAPrescriptionWithoutADocumentAsNotFound() {
		Prescription prescription = new Prescription();
		prescription.setPrescriptionId(7L);
		when(this.repository.findById(7L)).thenReturn(Optional.of(prescription));

		assertThatThrownBy(() -> this.service.getDocument(7L))
			.isInstanceOf(PrescriptionDocumentNotFoundException.class)
			.hasMessageContaining("7");
	}

	private void stubStorageOnly() {
		when(this.storage.store(anyString(), any(), anyLong(), anyString(), anyString()))
			.thenAnswer(invocation -> new PrescriptionDocument(invocation.getArgument(0), invocation.getArgument(3),
					invocation.getArgument(4), invocation.getArgument(2), null));
	}

	private void stubStorageSuccess() {
		stubStorageOnly();
		when(this.repository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private static MockMultipartFile jpeg(String filename) {
		return new MockMultipartFile("document", filename, "image/jpeg", TestDocuments.JPEG_BYTES);
	}

	private static PrescriptionCreateRequest request(EyeRequest right, EyeRequest left, boolean progressive,
			LocalDate issuedDate) {
		return new PrescriptionCreateRequest(7L, progressive, issuedDate, right, left, "Please use my usual frame");
	}

	private static EyeRequest eye(String sphere, String cylinder, String axis, String addPower) {
		return new EyeRequest(decimal(sphere), decimal(cylinder),
				axis == null ? null : Integer.valueOf(axis), decimal(addPower));
	}

	private static BigDecimal decimal(String value) {
		return value == null ? null : new BigDecimal(value);
	}

}
