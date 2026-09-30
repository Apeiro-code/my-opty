package com.myopty.order.service.storage;

import java.io.InputStream;

import com.myopty.order.domain.PrescriptionDocument;

/**
 * Store for prescription documents. The module depends on this interface rather
 * than on MinIO directly, so the backing store can be swapped without touching
 * the service or controller layers.
 */
public interface DocumentStorage {

	/**
	 * Persists the bytes and returns the reference to record alongside the
	 * prescription.
	 *
	 * @param objectKey destination key, already sanitised by the caller
	 * @param content   document bytes
	 * @param document  display metadata (original filename, size, upload time)
	 * @throws com.myopty.order.exception.DocumentStorageException if the store rejects the write
	 */
	PrescriptionDocument store(String objectKey, InputStream content, long contentLength, String filename,
			String contentType);

	/**
	 * Opens a stored document for streaming back to the client.
	 *
	 * @throws com.myopty.order.exception.DocumentStorageException if the object is missing
	 */
	DocumentContent open(String objectKey);

	/**
	 * Removes a stored document. Used to compensate for a failed database write
	 * and never fails hard: a leftover object is preferable to a failed request.
	 */
	void delete(String objectKey);

	/**
	 * A stored document plus the stream of its bytes. The caller closes the stream.
	 *
	 * @param sizeBytes byte length, or -1 when the store does not report it
	 */
	record DocumentContent(InputStream stream, String contentType, long sizeBytes) {
	}

}
