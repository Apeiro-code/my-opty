package com.myopty.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.myopty.order.domain.Prescription;
import com.myopty.order.domain.PrescriptionStatus;
import com.myopty.order.dto.PrescriptionResponse;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The entity to API projection, including the invariant that the private object
 * storage key never leaves the backend.
 */
class PrescriptionMapperTest {

	private static final String SECRET_OBJECT_KEY = "prescriptions/2026/09/6f1d0c7e-8a2b-4c3d-9e0f-1a2b3c4d5e6f.pdf";

	@Test
	void mapsEveryFieldOfAProgressivePrescription() {
		Prescription prescription = fullPrescription();

		PrescriptionResponse response = PrescriptionMapper.toResponse(prescription);

		assertThat(response.id()).isEqualTo(42L);
		assertThat(response.customerId()).isEqualTo(7L);
		assertThat(response.status()).isEqualTo("PENDING_REVIEW");
		assertThat(response.progressive()).isTrue();
		assertThat(response.issuedDate()).isEqualTo(LocalDate.of(2026, 3, 14));
		assertThat(response.notes()).isEqualTo("Please use my usual frame");
		assertThat(response.rightEye().sphere()).isEqualByComparingTo("-2.00");
		assertThat(response.rightEye().cylinder()).isEqualByComparingTo("-0.75");
		assertThat(response.rightEye().axis()).isEqualTo(180);
		assertThat(response.rightEye().addPower()).isEqualByComparingTo("2.00");
		assertThat(response.leftEye().sphere()).isEqualByComparingTo("-2.25");
		assertThat(response.leftEye().cylinder()).isNull();
		assertThat(response.leftEye().axis()).isNull();
		assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-03-14T10:15:30Z"));
		assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-03-14T10:15:30Z"));
	}

	@Test
	void mapsTheDocumentToItsDownloadPath() {
		PrescriptionResponse response = PrescriptionMapper.toResponse(fullPrescription());

		assertThat(response.document().filename()).isEqualTo("scan.pdf");
		assertThat(response.document().contentType()).isEqualTo("application/pdf");
		assertThat(response.document().sizeBytes()).isEqualTo(2048L);
		assertThat(response.document().uploadedAt()).isEqualTo(Instant.parse("2026-03-14T10:15:30Z"));
		assertThat(response.document().url()).isEqualTo("/api/prescriptions/42/document");
	}

	@Test
	void omitsTheDocumentWhenNoneWasUploaded() {
		Prescription prescription = new Prescription();
		prescription.setPrescriptionId(1L);

		assertThat(PrescriptionMapper.toResponse(prescription).document()).isNull();
	}

	@Test
	void treatsABlankObjectKeyAsNoDocument() {
		Prescription prescription = new Prescription();
		prescription.setDocumentObjectKey("   ");

		assertThat(prescription.hasDocument()).isFalse();
		assertThat(PrescriptionMapper.toResponse(prescription).document()).isNull();
	}

	/**
	 * The bucket is private, so the storage key must not be reachable from the API
	 * even indirectly, for example through a future field added to the response.
	 */
	@Test
	void neverSerialisesTheStorageKey() {
		JsonMapper mapper = JsonMapper.builder().build();

		String json = mapper.writeValueAsString(PrescriptionMapper.toResponse(fullPrescription()));

		assertThat(json).doesNotContain(SECRET_OBJECT_KEY)
			.doesNotContain("objectKey")
			.doesNotContain("object_key")
			.contains("/api/prescriptions/42/document");
	}

	@Test
	void reportsAMissingStatusAsNullRatherThanFailing() {
		Prescription prescription = new Prescription();
		prescription.setStatus(null);

		assertThat(PrescriptionMapper.toResponse(prescription).status()).isNull();
	}

	@Test
	void buildsTheDownloadPathFromTheIdentifier() {
		assertThat(PrescriptionMapper.downloadPath(99L)).isEqualTo("/api/prescriptions/99/document");
	}

	private static Prescription fullPrescription() {
		Prescription prescription = new Prescription();
		prescription.setPrescriptionId(42L);
		prescription.setCustomerId(7L);
		prescription.setProgressive(true);
		prescription.setStatus(PrescriptionStatus.PENDING_REVIEW);
		prescription.setIssuedDate(LocalDate.of(2026, 3, 14));
		prescription.setSphRight(new BigDecimal("-2.00"));
		prescription.setCylRight(new BigDecimal("-0.75"));
		prescription.setAxisRight(180);
		prescription.setAddRight(new BigDecimal("2.00"));
		prescription.setSphLeft(new BigDecimal("-2.25"));
		prescription.setNotes("Please use my usual frame");
		prescription.setDocumentObjectKey(SECRET_OBJECT_KEY);
		prescription.setDocumentFilename("scan.pdf");
		prescription.setDocumentContentType("application/pdf");
		prescription.setDocumentSizeBytes(2048L);
		prescription.setDocumentUploadedAt(Instant.parse("2026-03-14T10:15:30Z"));
		prescription.setCreatedAt(Instant.parse("2026-03-14T10:15:30Z"));
		prescription.setUpdatedAt(Instant.parse("2026-03-14T10:15:30Z"));
		return prescription;
	}

}
