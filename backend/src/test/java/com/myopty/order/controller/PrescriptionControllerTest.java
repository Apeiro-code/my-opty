package com.myopty.order.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

import com.myopty.order.domain.Prescription;
import com.myopty.order.domain.PrescriptionStatus;
import com.myopty.order.exception.DocumentStorageException;
import com.myopty.order.exception.PrescriptionDocumentNotFoundException;
import com.myopty.order.exception.PrescriptionNotFoundException;
import com.myopty.order.exception.OrderExceptionHandler;
import com.myopty.order.service.PrescriptionService;
import com.myopty.order.service.PrescriptionService.PrescriptionDocumentContent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Endpoint behaviour for the prescription form: the multipart contract, the
 * documented response envelope and the documented error codes.
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionControllerTest {

	private static final byte[] JPEG_BYTES = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46,
			0x49, 0x46, 0x00, 0x01 };

	private static final String VALID_REQUEST = """
			{
			  "customerId": 7,
			  "progressive": true,
			  "issuedDate": "2026-03-14",
			  "rightEye": { "sphere": -2.00, "cylinder": -0.75, "axis": 180, "addPower": 2.00 },
			  "leftEye":  { "sphere": -2.25, "addPower": 2.00 },
			  "notes": "Please use my usual frame"
			}
			""";

	@Mock
	private PrescriptionService service;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.standaloneSetup(new PrescriptionController(this.service))
			.setControllerAdvice(new OrderExceptionHandler())
			.build();
	}

	@Test
	void acceptsAMultipartSubmission() throws Exception {
		when(this.service.create(any(), any())).thenReturn(prescription());

		this.mockMvc.perform(multipart("/api/prescriptions")
			.file(new MockMultipartFile("prescription", "", MediaType.APPLICATION_JSON_VALUE, VALID_REQUEST.getBytes(StandardCharsets.UTF_8)))
			.file(new MockMultipartFile("document", "rx-scan.jpg", MediaType.IMAGE_JPEG_VALUE, JPEG_BYTES)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(12L))
			.andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
			.andExpect(jsonPath("$.data.progressive").value(true))
			.andExpect(jsonPath("$.data.issuedDate").value("2026-03-14"))
			.andExpect(jsonPath("$.data.rightEye.sphere").value(-2.00))
			.andExpect(jsonPath("$.data.rightEye.cylinder").value(-0.75))
			.andExpect(jsonPath("$.data.rightEye.axis").value(180))
			.andExpect(jsonPath("$.data.rightEye.addPower").value(2.00))
			.andExpect(jsonPath("$.data.leftEye.sphere").value(-2.25))
			.andExpect(jsonPath("$.data.notes").value("Please use my usual frame"))
			.andExpect(jsonPath("$.data.document.filename").value("rx-scan.jpg"))
			.andExpect(jsonPath("$.data.document.url").value("/api/prescriptions/12/document"))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void neverExposesTheObjectStorageKey() throws Exception {
		when(this.service.create(any(), any())).thenReturn(prescription());

		this.mockMvc.perform(multipart("/api/prescriptions")
			.file(new MockMultipartFile("prescription", "", MediaType.APPLICATION_JSON_VALUE, VALID_REQUEST.getBytes(StandardCharsets.UTF_8)))
			.file(new MockMultipartFile("document", "rx-scan.jpg", MediaType.IMAGE_JPEG_VALUE, JPEG_BYTES)))
			.andExpect(status().isCreated())
			.andExpect(content().string(not(containsString("prescriptions/2026/09/"))));
	}

	@Test
	void rejectsOpticalValuesOutsideTheAllowedRange() throws Exception {
		String request = """
				{
				  "rightEye": { "sphere": -45.00 },
				  "leftEye": { "sphere": -2.00 }
				}
				""";

		this.mockMvc.perform(multipart("/api/prescriptions")
			.file(new MockMultipartFile("prescription", "", MediaType.APPLICATION_JSON_VALUE, request.getBytes(StandardCharsets.UTF_8)))
			.file(new MockMultipartFile("document", "rx.jpg", MediaType.IMAGE_JPEG_VALUE, JPEG_BYTES)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.error.fieldErrors['rightEye.sphere']").exists());
	}

	@Test
	void rejectsASubmissionWithoutAnEye() throws Exception {
		String request = """
				{ "rightEye": { "sphere": -2.00 } }
				""";

		this.mockMvc.perform(multipart("/api/prescriptions")
			.file(new MockMultipartFile("prescription", "", MediaType.APPLICATION_JSON_VALUE, request.getBytes(StandardCharsets.UTF_8)))
			.file(new MockMultipartFile("document", "rx.jpg", MediaType.IMAGE_JPEG_VALUE, JPEG_BYTES)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.fieldErrors.leftEye").exists());
	}

	@Test
	void rejectsASubmissionWithoutADocumentPart() throws Exception {
		this.mockMvc.perform(multipart("/api/prescriptions")
			.file(new MockMultipartFile("prescription", "", MediaType.APPLICATION_JSON_VALUE, VALID_REQUEST.getBytes(StandardCharsets.UTF_8))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("MISSING_PART"))
			.andExpect(jsonPath("$.error.fieldErrors.document").exists());
	}

	@Test
	void returnsASinglePrescription() throws Exception {
		when(this.service.getById(12L)).thenReturn(prescription());

		this.mockMvc.perform(get("/api/prescriptions/12"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(12L))
			.andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
	}

	@Test
	void reportsAnUnknownPrescription() throws Exception {
		when(this.service.getById(99L)).thenThrow(new PrescriptionNotFoundException(99L));

		this.mockMvc.perform(get("/api/prescriptions/99"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("PRESCRIPTION_NOT_FOUND"))
			.andExpect(jsonPath("$.error.message").value("Prescription 99 was not found"));
	}

	@Test
	void streamsTheUploadedDocument() throws Exception {
		when(this.service.getDocument(12L)).thenReturn(new PrescriptionDocumentContent("rx-scan.jpg", "image/jpeg",
				JPEG_BYTES.length, new ByteArrayInputStream(JPEG_BYTES)));

		this.mockMvc.perform(get("/api/prescriptions/12/document"))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.IMAGE_JPEG))
			.andExpect(content().bytes(JPEG_BYTES))
			.andExpect(header().string("Content-Disposition", containsString("rx-scan.jpg")));
	}

	@Test
	void reportsAPrescriptionWithoutADocumentAsNotFound() throws Exception {
		when(this.service.getDocument(12L)).thenThrow(new PrescriptionDocumentNotFoundException(12L));

		this.mockMvc.perform(get("/api/prescriptions/12/document"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("DOCUMENT_NOT_FOUND"));
	}

	@Test
	void reportsAStorageFailureAsRetryable() throws Exception {
		when(this.service.getDocument(12L)).thenThrow(new DocumentStorageException("minio down"));

		this.mockMvc.perform(get("/api/prescriptions/12/document"))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("DOCUMENT_STORAGE_UNAVAILABLE"));
	}

	private static Prescription prescription() {
		Prescription prescription = new Prescription();
		prescription.setPrescriptionId(12L);
		prescription.setCustomerId(7L);
		prescription.setProgressive(true);
		prescription.setIssuedDate(LocalDate.of(2026, 3, 14));
		prescription.setSphRight(new BigDecimal("-2.00"));
		prescription.setCylRight(new BigDecimal("-0.75"));
		prescription.setAxisRight(180);
		prescription.setAddRight(new BigDecimal("2.00"));
		prescription.setSphLeft(new BigDecimal("-2.25"));
		prescription.setAddLeft(new BigDecimal("2.00"));
		prescription.setNotes("Please use my usual frame");
		prescription.setStatus(PrescriptionStatus.PENDING_REVIEW);
		prescription.setDocumentObjectKey("prescriptions/2026/09/2f0c0a3e-1f2b-4c8a-9d1e-0a1b2c3d4e5f.jpg");
		prescription.setDocumentFilename("rx-scan.jpg");
		prescription.setDocumentContentType("image/jpeg");
		prescription.setDocumentSizeBytes((long) JPEG_BYTES.length);
		prescription.setDocumentUploadedAt(Instant.parse("2026-09-29T10:15:30Z"));
		prescription.setCreatedAt(Instant.parse("2026-09-29T10:15:30Z"));
		prescription.setUpdatedAt(Instant.parse("2026-09-29T10:15:30Z"));
		return prescription;
	}

}
