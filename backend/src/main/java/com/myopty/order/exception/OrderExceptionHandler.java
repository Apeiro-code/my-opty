package com.myopty.order.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import com.myopty.order.dto.ApiResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Maps Order &amp; Prescription failures onto the standard error envelope.
 *
 * <p>Scoped to this module's exceptions so it cannot change how the other
 * modules report errors. A global handler belongs in {@code com.myopty.shared}.
 */
@RestControllerAdvice(basePackages = "com.myopty.order.controller")
public class OrderExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(OrderExceptionHandler.class);

	@ExceptionHandler(PrescriptionNotFoundException.class)
	ResponseEntity<ApiResponse<Void>> handleNotFound(PrescriptionNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiResponse.error("PRESCRIPTION_NOT_FOUND", ex.getMessage()));
	}

	/**
	 * The prescription is there, the document is not. Reporting this as a storage
	 * failure would tell the client to retry something that will never succeed.
	 */
	@ExceptionHandler(PrescriptionDocumentNotFoundException.class)
	ResponseEntity<ApiResponse<Void>> handleDocumentNotFound(PrescriptionDocumentNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiResponse.error("DOCUMENT_NOT_FOUND", ex.getMessage()));
	}

	@ExceptionHandler(InvalidPrescriptionException.class)
	ResponseEntity<ApiResponse<Void>> handleInvalidPrescription(InvalidPrescriptionException ex) {
		return ResponseEntity.badRequest()
			.body(ApiResponse.error("INVALID_PRESCRIPTION", ex.getMessage(), ex.getFieldErrors()));
	}

	@ExceptionHandler(InvalidPrescriptionDocumentException.class)
	ResponseEntity<ApiResponse<Void>> handleInvalidDocument(InvalidPrescriptionDocumentException ex) {
		return ResponseEntity.badRequest()
			.body(ApiResponse.error("INVALID_DOCUMENT", ex.getMessage(), Map.of("document", ex.getMessage())));
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	ResponseEntity<ApiResponse<Void>> handleMissingPart(MissingServletRequestPartException ex) {
		String message = "The " + ex.getRequestPartName() + " part is required";
		return ResponseEntity.badRequest()
			.body(ApiResponse.error("MISSING_PART", message, Map.of(ex.getRequestPartName(), "is required")));
	}

	/**
	 * Bean validation on the {@code prescription} part. {@link BindException} also
	 * covers {@code MethodArgumentNotValidException}, which is what Spring MVC
	 * raises for a request part.
	 */
	@ExceptionHandler(BindException.class)
	ResponseEntity<ApiResponse<Void>> handleValidation(BindException ex) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.merge(error.getField(), error.getDefaultMessage(),
					(existing, next) -> existing + "; " + next);
		}
		for (var error : ex.getBindingResult().getGlobalErrors()) {
			fieldErrors.putIfAbsent(error.getObjectName(), error.getDefaultMessage());
		}
		return ResponseEntity.badRequest()
			.body(ApiResponse.error("VALIDATION_FAILED", "Some prescription values are not valid", fieldErrors));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<ApiResponse<Void>> handleTooLarge(MaxUploadSizeExceededException ex) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
			.body(ApiResponse.error("DOCUMENT_TOO_LARGE", "The prescription document is too large", null));
	}

	@ExceptionHandler({ MultipartException.class, MissingServletRequestParameterException.class })
	ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception ex) {
		logger.debug("Rejected malformed prescription request", ex);
		return ResponseEntity.badRequest()
			.body(ApiResponse.error("MALFORMED_REQUEST", "The prescription request could not be read", null));
	}

	/**
	 * Object storage is a downstream dependency, so its failure is reported as a
	 * bad gateway: the customer can retry without re-entering anything.
	 */
	@ExceptionHandler(DocumentStorageException.class)
	ResponseEntity<ApiResponse<Void>> handleStorageFailure(DocumentStorageException ex) {
		logger.error("Prescription document storage is unavailable", ex);
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.error("DOCUMENT_STORAGE_UNAVAILABLE",
				"The prescription document could not be stored right now. Please try again.", null));
	}

}
