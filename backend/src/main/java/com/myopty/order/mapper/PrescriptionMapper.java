package com.myopty.order.mapper;

import com.myopty.order.domain.EyePrescription;
import com.myopty.order.domain.Prescription;
import com.myopty.order.domain.PrescriptionDocument;
import com.myopty.order.dto.PrescriptionResponse;
import com.myopty.order.dto.PrescriptionResponse.DocumentResponse;
import com.myopty.order.dto.PrescriptionResponse.EyeResponse;

/**
 * Converts a stored {@link Prescription} into its API representation. The object
 * storage key is deliberately not mapped: clients only ever get the download path.
 */
public final class PrescriptionMapper {

	private PrescriptionMapper() {
	}

	public static PrescriptionResponse toResponse(Prescription prescription) {
		return new PrescriptionResponse(prescription.getPrescriptionId(), prescription.getCustomerId(),
				prescription.getStatus() == null ? null : prescription.getStatus().name(), prescription.isProgressive(),
				prescription.getIssuedDate(), toEyeResponse(prescription.rightEye()), toEyeResponse(prescription.leftEye()),
				prescription.getNotes(), prescription.getRejectionReason(), toDocumentResponse(prescription),
				prescription.getCreatedAt(), prescription.getUpdatedAt());
	}

	private static EyeResponse toEyeResponse(EyePrescription eye) {
		return new EyeResponse(eye.sphere(), eye.cylinder(), eye.axis(), eye.add());
	}

	private static DocumentResponse toDocumentResponse(Prescription prescription) {
		PrescriptionDocument document = prescription.document();
		if (document == null) {
			return null;
		}
		return new DocumentResponse(document.filename(), document.contentType(), document.sizeBytes(),
				prescription.getDocumentUploadedAt(), downloadPath(prescription.getPrescriptionId()));
	}

	/**
	 * @return the path the client uses to fetch the document; the bucket is private
	 *         so there is no object URL to hand out
	 */
	public static String downloadPath(Long prescriptionId) {
		return "/api/prescriptions/" + prescriptionId + "/document";
	}

}
