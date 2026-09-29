package com.myopty.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for the S3-compatible object store that holds prescription
 * documents, bound from {@code myopty.documents.storage.minio.*}.
 */
@ConfigurationProperties(prefix = "myopty.documents.storage.minio")
public record MinioProperties(String endpoint, String accessKey, String secretKey, String bucket,
		boolean autoCreateBucket) {

}
