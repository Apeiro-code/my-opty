package com.myopty.order.controller;

import java.nio.charset.StandardCharsets;

import com.myopty.order.domain.Prescription;
import com.myopty.order.dto.ApiResponse;
import com.myopty.order.dto.PrescriptionCreateRequest;
import com.myopty.order.dto.PrescriptionResponse;
import com.myopty.order.mapper.PrescriptionMapper;
import com.myopty.order.service.PrescriptionService;
import com.myopty.order.service.PrescriptionService.PrescriptionDocumentContent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Prescription endpoints for the Order &amp; Prescription module.
 *
 * <p>Submission is a {@code multipart/form-data} request: the {@code prescription}
 * part carries the optical values as JSON, the {@code document} part carries the
 * scan or photo the shop verifies them against. The Swagger
 * {@code io.swagger...ApiResponse} annotation clashes with the
 * {@link ApiResponse} envelope, so it is referenced by its fully qualified name.
 */
@RestController
@RequestMapping("/api/prescriptions")
@Tag(name = "Prescriptions", description = "Submit and retrieve customer prescriptions")
public class PrescriptionController {

	private final PrescriptionService service;

	public PrescriptionController(PrescriptionService service) {
		this.service = service;
	}

	@Operation(summary = "Submit a prescription with its supporting document",
			description = "Stores the typed optical values together with a scan or photo of the prescription. "
					+ "The prescription is created with status PENDING_REVIEW for the shop to verify.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Prescription submitted"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
					description = "Invalid optical values or unsupported document"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413",
					description = "Document larger than the configured limit"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502",
					description = "Document storage unavailable") })
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<PrescriptionResponse>> create(
			@Parameter(description = "Optical values per eye", required = true) //
			@Valid @RequestPart("prescription") PrescriptionCreateRequest request,
			@Parameter(description = "Scan or photo of the prescription (PDF, JPEG, PNG, WebP or HEIC)", required = true) //
			@RequestPart("document") MultipartFile document) {
		Prescription created = this.service.create(request, document);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(PrescriptionMapper.toResponse(created)));
	}

	@Operation(summary = "View a prescription")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescription found"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
					description = "Prescription not found") })
	@GetMapping("/{prescriptionId}")
	public ApiResponse<PrescriptionResponse> getById(
			@Parameter(description = "Prescription id", required = true) @PathVariable Long prescriptionId) {
		return ApiResponse.ok(PrescriptionMapper.toResponse(this.service.getById(prescriptionId)));
	}

	@Operation(summary = "Download the uploaded prescription document",
			description = "Streams the stored scan or photo so the shop can verify it. Never exposes the storage key.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Document bytes",
					content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
					description = "Prescription not found, or it has no document attached"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502",
					description = "Document storage unavailable") })
	@GetMapping("/{prescriptionId}/document")
	public ResponseEntity<InputStreamResource> downloadDocument(
			@Parameter(description = "Prescription id", required = true) @PathVariable Long prescriptionId) {
		PrescriptionDocumentContent document = this.service.getDocument(prescriptionId);
		ResponseEntity.BodyBuilder response = ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(document.contentType()))
			.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.attachment().filename(document.filename(), StandardCharsets.UTF_8).build().toString());
		if (document.sizeBytes() >= 0) {
			response.contentLength(document.sizeBytes());
		}
		return response.body(new InputStreamResource(document.stream()));
	}

}
