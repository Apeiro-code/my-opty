package com.myopty.order.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.myopty.order.domain.EyePrescription;
import com.myopty.order.domain.Prescription;
import com.myopty.order.domain.PrescriptionDocument;
import com.myopty.order.domain.PrescriptionStatus;
import com.myopty.order.dto.EyeRequest;
import com.myopty.order.dto.PrescriptionCreateRequest;
import com.myopty.order.exception.DocumentStorageException;
import com.myopty.order.exception.InvalidPrescriptionException;
import com.myopty.order.exception.PrescriptionDocumentNotFoundException;
import com.myopty.order.exception.PrescriptionNotFoundException;
import com.myopty.order.repository.PrescriptionRepository;
import com.myopty.order.service.storage.DocumentStorage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Default {@link PrescriptionService}.
 *
 * <p>The optical values and the uploaded document must agree, so the document is
 * stored first and the row is written second. If the write fails the stored object
 * is removed, so a failed submission leaves nothing behind and the customer can
 * simply try again.
 */
@Service
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

	private static final DateTimeFormatter KEY_DATE = DateTimeFormatter.ofPattern("yyyy/MM").withZone(ZoneOffset.UTC);

	private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of("image/jpeg", ".jpg", "image/png", ".png",
			"image/webp", ".webp", "image/heic", ".heic", "application/pdf", ".pdf");

	private final PrescriptionRepository repository;

	private final DocumentStorage storage;

	private final DocumentValidator documentValidator;

	public PrescriptionServiceImpl(PrescriptionRepository repository, DocumentStorage storage,
			DocumentValidator documentValidator) {
		this.repository = repository;
		this.storage = storage;
		this.documentValidator = documentValidator;
	}

	@Override
	public Prescription create(PrescriptionCreateRequest request, MultipartFile document) {
		validateOpticalValues(request);
		String contentType = this.documentValidator.validate(document);

		Prescription prescription = toPrescription(request);
		String objectKey = buildObjectKey(contentType);
		PrescriptionDocument stored = storeDocument(objectKey, document, contentType);
		applyDocument(prescription, stored);

		try {
			return this.repository.save(prescription);
		}
		catch (RuntimeException ex) {
			this.storage.delete(objectKey);
			throw ex;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Prescription getById(Long prescriptionId) {
		return this.repository.findById(prescriptionId)
			.orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));
	}

	@Override
	@Transactional(readOnly = true)
	public PrescriptionDocumentContent getDocument(Long prescriptionId) {
		Prescription prescription = getById(prescriptionId);
		PrescriptionDocument stored = prescription.document();
		if (stored == null) {
			throw new PrescriptionDocumentNotFoundException(prescriptionId);
		}
		DocumentStorage.DocumentContent content = this.storage.open(stored.objectKey());
		return new PrescriptionDocumentContent(stored.filename(), content.contentType(), content.sizeBytes(),
				content.stream());
	}

	private PrescriptionDocument storeDocument(String objectKey, MultipartFile document, String contentType) {
		try (var in = document.getInputStream()) {
			return this.storage.store(objectKey, in, document.getSize(), sanitiseFilename(document.getOriginalFilename()),
					contentType);
		}
		catch (IOException ex) {
			throw new DocumentStorageException("The prescription document could not be read", ex);
		}
	}

	/**
	 * Keys documents by upload month and a random UUID. The extension comes from
	 * the detected media type and never from the uploaded filename, so a
	 * {@code .exe} or a path traversal attempt cannot influence the stored name.
	 */
	private static String buildObjectKey(String contentType) {
		String extension = EXTENSION_BY_CONTENT_TYPE.getOrDefault(contentType, ".bin");
		return "prescriptions/" + KEY_DATE.format(Instant.now()) + "/" + UUID.randomUUID() + extension;
	}

	private static void applyDocument(Prescription prescription, PrescriptionDocument stored) {
		prescription.setDocumentObjectKey(stored.objectKey());
		prescription.setDocumentFilename(stored.filename());
		prescription.setDocumentContentType(stored.contentType());
		prescription.setDocumentSizeBytes(stored.sizeBytes());
		prescription.setDocumentUploadedAt(Instant.now());
	}

	/**
	 * Keeps only the last path segment of a client-supplied name so it cannot be
	 * used to escape the metadata column or to forge a Content-Disposition header.
	 */
	static String sanitiseFilename(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			return "prescription-document";
		}
		String name = originalFilename.replace('\\', '/');
		int slash = name.lastIndexOf('/');
		if (slash >= 0) {
			name = name.substring(slash + 1);
		}
		name = name.replaceAll("[\\r\\n\"]", "_").trim();
		if (name.isEmpty() || name.equals(".") || name.equals("..")) {
			return "prescription-document";
		}
		return name.length() > 255 ? name.substring(0, 255) : name;
	}

	private static Prescription toPrescription(PrescriptionCreateRequest request) {
		EyePrescription right = toEyePrescription(request.rightEye());
		EyePrescription left = toEyePrescription(request.leftEye());
		Instant now = Instant.now();

		Prescription prescription = new Prescription();
		prescription.setCustomerId(request.customerId());
		prescription.setProgressive(Boolean.TRUE.equals(request.progressive()));
		prescription.setIssuedDate(request.issuedDate());
		prescription.setSphRight(right.sphere());
		prescription.setCylRight(right.cylinder());
		prescription.setAxisRight(right.axis());
		prescription.setAddRight(right.add());
		prescription.setSphLeft(left.sphere());
		prescription.setCylLeft(left.cylinder());
		prescription.setAxisLeft(left.axis());
		prescription.setAddLeft(left.add());
		prescription.setNotes(trimToNull(request.notes()));
		prescription.setStatus(PrescriptionStatus.PENDING_REVIEW);
		prescription.setCreatedAt(now);
		prescription.setUpdatedAt(now);
		return prescription;
	}

	private static EyePrescription toEyePrescription(EyeRequest request) {
		return new EyePrescription(EyePrescription.normalise(request.sphere()),
				EyePrescription.normalise(request.cylinder()), request.axis(),
				EyePrescription.normaliseAdd(request.addPower()));
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/**
	 * Enforces the rules that span more than one field, so they hold no matter
	 * which endpoint or test calls the service.
	 */
	private void validateOpticalValues(PrescriptionCreateRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		validateEye("rightEye", request.rightEye(), Boolean.TRUE.equals(request.progressive()), errors);
		validateEye("leftEye", request.leftEye(), Boolean.TRUE.equals(request.progressive()), errors);
		if (!errors.isEmpty()) {
			throw new InvalidPrescriptionException("Some prescription values are not valid", errors);
		}
	}

	private static void validateEye(String field, EyeRequest eye, boolean progressive, Map<String, String> errors) {
		if (eye == null) {
			return;
		}
		EyePrescription values = new EyePrescription(EyePrescription.normalise(eye.sphere()),
				EyePrescription.normalise(eye.cylinder()), eye.axis(), EyePrescription.normaliseAdd(eye.addPower()));

		checkRange(field, "sphere", values.sphere(), EyePrescription.MIN_SPHERE, EyePrescription.MAX_SPHERE, errors);
		checkRange(field, "cylinder", values.cylinder(), EyePrescription.MIN_CYLINDER, EyePrescription.MAX_CYLINDER,
				errors);
		checkRange(field, "addPower", values.add(), EyePrescription.NO_ADD, EyePrescription.MAX_ADD, errors);
		checkQuarterStep(field, "sphere", values.sphere(), errors);
		checkQuarterStep(field, "cylinder", values.cylinder(), errors);
		checkQuarterStep(field, "addPower", values.add(), errors);
		checkAxis(field, values, errors);
		checkAddPower(field, values, progressive, errors);
	}

	private static void checkRange(String field, String name, BigDecimal value, BigDecimal min, BigDecimal max,
			Map<String, String> errors) {
		if (!EyePrescription.isWithin(value, min, max)) {
			errors.put(field + "." + name, "must be between " + min.toPlainString() + " and " + max.toPlainString());
		}
	}

	private static void checkQuarterStep(String field, String name, BigDecimal value, Map<String, String> errors) {
		if (!EyePrescription.isQuarterStep(value)) {
			errors.put(field + "." + name, "must be in 0.25 dioptre steps");
		}
	}

	/**
	 * An axis is meaningless without astigmatism, and an axis without a cylinder
	 * is a data-entry slip the shop would otherwise have to query about.
	 */
	private static void checkAxis(String field, EyePrescription values, Map<String, String> errors) {
		boolean axisProvided = values.axis() != null && values.axis() != 0;
		if (values.hasCylinder() && values.axis() == null) {
			errors.put(field + ".axis", "is required when the cylinder is not 0.00");
		}
		else if (!values.hasCylinder() && axisProvided) {
			errors.put(field + ".axis", "must be 0 or blank when the cylinder is 0.00");
		}
		if (values.axis() != null && (values.axis() < EyePrescription.MIN_AXIS || values.axis() > EyePrescription.MAX_AXIS)) {
			errors.put(field + ".axis", "must be between " + EyePrescription.MIN_AXIS + " and " + EyePrescription.MAX_AXIS);
		}
	}

	/**
	 * A progressive lens needs a near addition on both eyes, otherwise the lens
	 * cannot be manufactured as ordered. A blank or {@code 0.00} addition means
	 * "none", which progressive customers have not supplied.
	 */
	private static void checkAddPower(String field, EyePrescription values, boolean progressive,
			Map<String, String> errors) {
		if (progressive && !values.hasAdd()) {
			errors.put(field + ".addPower", "is required for a progressive lens");
		}
	}

}
