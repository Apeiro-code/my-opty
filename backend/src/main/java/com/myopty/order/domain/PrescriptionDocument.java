package com.myopty.order.domain;

import java.time.Instant;

/**
 * Metadata of the prescription document a customer uploaded. The bytes live in
 * object storage; only the reference and the display metadata are kept here.
 *
 * @param objectKey   private storage key, never exposed to the client
 * @param filename    original name, shown to the customer and the client for display
 * @param contentType detected media type, sniffed from the file signature
 * @param sizeBytes   size in bytes, enforced at upload time
 * @param uploadedAt  when the document was accepted
 */
public record PrescriptionDocument(String objectKey, String filename, String contentType, Long sizeBytes,
		Instant uploadedAt) {

}
