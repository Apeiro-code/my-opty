package com.myopty.order.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import com.myopty.order.config.DocumentProperties;
import com.myopty.order.exception.InvalidPrescriptionDocumentException;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Checks an uploaded prescription document before anything is persisted.
 *
 * <p>The declared {@code Content-Type} comes from the browser and cannot be
 * trusted, so the media type is re-derived from the file signature. Only then is
 * the document allowed anywhere near object storage.
 */
@Component
public class DocumentValidator {

	private static final int SIGNATURE_LENGTH = 12;

	private final DocumentProperties properties;

	public DocumentValidator(DocumentProperties properties) {
		this.properties = properties;
	}

	/**
	 * @return the media type detected from the file signature, never the
	 *         client-declared one
	 * @throws InvalidPrescriptionDocumentException if the file is missing, empty,
	 *         too large, or not an accepted scan or photo
	 */
	public String validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidPrescriptionDocumentException("A scan or photo of your prescription is required");
		}
		if (file.getSize() > this.properties.maxSizeBytes()) {
			throw new InvalidPrescriptionDocumentException("The prescription document must be smaller than "
					+ (this.properties.maxSizeBytes() / (1024 * 1024)) + " MB");
		}
		String declared = normalise(file.getContentType());
		String detected = detectContentType(file);
		if (detected == null) {
			throw new InvalidPrescriptionDocumentException(
					"The prescription document must be a PDF, JPEG, PNG, WebP or HEIC file");
		}
		if (!this.properties.contentTypes().contains(detected)) {
			throw new InvalidPrescriptionDocumentException(
					"The prescription document must be a PDF, JPEG, PNG, WebP or HEIC file");
		}
		if (declared != null && !declared.equals(detected) && !isGeneric(declared)) {
			throw new InvalidPrescriptionDocumentException("The prescription document type does not match its content");
		}
		return detected;
	}

	private static boolean isGeneric(String contentType) {
		return List.of("application/octet-stream", "binary/octet-stream").contains(contentType);
	}

	private static String normalise(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return null;
		}
		String value = contentType.toLowerCase(Locale.ROOT);
		int parameters = value.indexOf(';');
		return (parameters >= 0 ? value.substring(0, parameters) : value).trim();
	}

	/**
	 * Identifies the accepted formats from their leading bytes. HEIC/HEIF is
	 * detected from the {@code ftyp} box brand, which is how the format itself is
	 * defined rather than an extension.
	 */
	private static String detectContentType(MultipartFile file) {
		byte[] header = new byte[SIGNATURE_LENGTH];
		try (InputStream in = file.getInputStream()) {
			int read = in.readNBytes(header, 0, SIGNATURE_LENGTH);
			if (read < SIGNATURE_LENGTH) {
				return null;
			}
		}
		catch (IOException ex) {
			throw new InvalidPrescriptionDocumentException("The prescription document could not be read", ex);
		}
		return detectContentType(header);
	}

	static String detectContentType(byte[] header) {
		if (startsWith(header, 0, 0xFF, 0xD8, 0xFF)) {
			return "image/jpeg";
		}
		if (startsWith(header, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
			return "image/png";
		}
		if (startsWith(header, 0, 0x25, 0x50, 0x44, 0x46)) {
			return "application/pdf";
		}
		if (startsWith(header, 0, 'R', 'I', 'F', 'F') && matchesAt(header, 8, "WEBP")) {
			return "image/webp";
		}
		if (matchesAt(header, 4, "ftyp") && (matchesAt(header, 8, "heic") || matchesAt(header, 8, "heix")
				|| matchesAt(header, 8, "hevc") || matchesAt(header, 8, "mif1"))) {
			return "image/heic";
		}
		return null;
	}

	private static boolean startsWith(byte[] header, int offset, int... signature) {
		if (header.length < offset + signature.length) {
			return false;
		}
		for (int i = 0; i < signature.length; i++) {
			if ((header[offset + i] & 0xFF) != (signature[i] & 0xFF)) {
				return false;
			}
		}
		return true;
	}

	private static boolean matchesAt(byte[] header, int offset, String text) {
		if (header.length < offset + text.length()) {
			return false;
		}
		for (int i = 0; i < text.length(); i++) {
			if ((header[offset + i] & 0xFF) != text.charAt(i)) {
				return false;
			}
		}
		return true;
	}

}
