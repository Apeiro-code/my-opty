package com.myopty.order.service;

import java.util.List;

import com.myopty.order.config.DocumentProperties;

/**
 * Fixtures shared by the order module tests: the accepted media types, a document
 * size limit and file bytes that carry a real signature.
 */
final class TestDocuments {

	static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

	static final byte[] JPEG_BYTES = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46,
			0x49, 0x46, 0x00, 0x01 };

	static final byte[] PNG_BYTES = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D };

	static final byte[] PDF_BYTES = { 0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37, 0x0A, 0x25, (byte) 0xE2,
			(byte) 0xE3 };

	static final byte[] EXECUTABLE_BYTES = { 0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00,
			0x00 };

	private TestDocuments() {
	}

	static DocumentProperties properties() {
		return new DocumentProperties(MAX_SIZE_BYTES,
				List.of("image/jpeg", "image/png", "image/webp", "image/heic", "application/pdf"));
	}

}
