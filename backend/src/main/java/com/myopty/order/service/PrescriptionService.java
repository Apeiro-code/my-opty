package com.myopty.order.service;

import java.io.InputStream;

import com.myopty.order.domain.Prescription;
import com.myopty.order.dto.PrescriptionCreateRequest;

import org.springframework.web.multipart.MultipartFile;

/**
 * Prescription submission and retrieval, including the prescription document the
 * customer uploads for the shop to verify.
 */
public interface PrescriptionService {

	/**
	 * Stores the prescription and its document.
	 *
	 * @param request  optical values per eye; a customer cannot choose the status,
	 *                 it is always {@code PENDING_REVIEW}
	 * @param document scan or photo of the prescription, required so the shop can
	 *                 verify the typed values
	 * @throws com.myopty.order.exception.InvalidPrescriptionException      if an optical value breaks a prescription rule
	 * @throws com.myopty.order.exception.InvalidPrescriptionDocumentException if the document is missing or unusable
	 */
	Prescription create(PrescriptionCreateRequest request, MultipartFile document);

	/**
	 * @throws com.myopty.order.exception.PrescriptionNotFoundException if no such prescription exists
	 */
	Prescription getById(Long prescriptionId);

	/**
	 * Opens the uploaded prescription document for the shop to verify.
	 *
	 * @throws com.myopty.order.exception.PrescriptionNotFoundException if no such prescription exists
	 * @throws com.myopty.order.exception.DocumentStorageException      if no document was uploaded or it cannot be read
	 */
	PrescriptionDocumentContent getDocument(Long prescriptionId);

	/**
	 * @param filename    sanitised original filename, safe for Content-Disposition
	 * @param contentType media type detected at upload time
	 * @param sizeBytes   byte length, or -1 when unknown
	 * @param stream      document bytes; the caller closes the stream
	 */
	record PrescriptionDocumentContent(String filename, String contentType, long sizeBytes, InputStream stream) {
	}

}
