package com.myopty.order.service.storage;

import java.io.InputStream;

import com.myopty.order.config.MinioProperties;
import com.myopty.order.domain.PrescriptionDocument;
import com.myopty.order.exception.DocumentStorageException;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * {@link DocumentStorage} backed by MinIO over the S3 API.
 *
 * <p>The bucket is private: documents are only ever served through
 * {@code GET /api/prescriptions/{id}/document}, never by a public object URL.
 */
@Component
public class MinioDocumentStorage implements DocumentStorage {

	private static final Logger logger = LoggerFactory.getLogger(MinioDocumentStorage.class);

	private static final long UPLOAD_PART_SIZE = 10L * 1024 * 1024;

	private final MinioClient client;

	private final MinioProperties properties;

	public MinioDocumentStorage(MinioClient client, MinioProperties properties) {
		this.client = client;
		this.properties = properties;
	}

	/**
	 * Creates the bucket on first start when configured to. A store that is not up
	 * yet must not stop the application from booting: the failure surfaces on the
	 * first upload instead, where the customer gets a retryable error.
	 */
	@PostConstruct
	void ensureBucket() {
		if (!this.properties.autoCreateBucket()) {
			return;
		}
		try {
			boolean exists = this.client.bucketExists(BucketExistsArgs.builder().bucket(this.properties.bucket()).build());
			if (!exists) {
				this.client.makeBucket(MakeBucketArgs.builder().bucket(this.properties.bucket()).build());
				logger.info("Created prescription document bucket '{}'", this.properties.bucket());
			}
		}
		catch (Exception ex) {
			logger.warn("Object storage at {} is not reachable yet: {}", this.properties.endpoint(), ex.getMessage());
		}
	}

	@Override
	public PrescriptionDocument store(String objectKey, InputStream content, long contentLength, String filename,
			String contentType) {
		try {
			this.client.putObject(PutObjectArgs.builder()
				.bucket(this.properties.bucket())
				.object(objectKey)
				.stream(content, contentLength, UPLOAD_PART_SIZE)
				.contentType(contentType)
				.build());
			return new PrescriptionDocument(objectKey, filename, contentType, contentLength, null);
		}
		catch (Exception ex) {
			throw new DocumentStorageException("Could not store the prescription document", ex);
		}
	}

	@Override
	public DocumentContent open(String objectKey) {
		try {
			var stat = this.client.statObject(
					StatObjectArgs.builder().bucket(this.properties.bucket()).object(objectKey).build());
			InputStream stream = this.client
				.getObject(GetObjectArgs.builder().bucket(this.properties.bucket()).object(objectKey).build());
			String contentType = stat.contentType() == null ? "application/octet-stream" : stat.contentType();
			return new DocumentContent(stream, contentType, stat.size());
		}
		catch (Exception ex) {
			throw new DocumentStorageException("The stored prescription document could not be read", ex);
		}
	}

	@Override
	public void delete(String objectKey) {
		try {
			this.client.removeObject(
					RemoveObjectArgs.builder().bucket(this.properties.bucket()).object(objectKey).build());
		}
		catch (Exception ex) {
			logger.warn("Could not remove orphaned prescription document {}: {}", objectKey, ex.getMessage());
		}
	}

}
