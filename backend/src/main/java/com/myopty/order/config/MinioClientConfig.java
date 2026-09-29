package com.myopty.order.config;

import io.minio.MinioClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the object storage client used by the Order &amp; Prescription module.
 * The credentials and bucket live in configuration, never in code.
 */
@Configuration(proxyBeanMethods = false)
public class MinioClientConfig {

	@Bean
	MinioClient minioClient(MinioProperties properties) {
		return MinioClient.builder()
			.endpoint(properties.endpoint())
			.credentials(properties.accessKey(), properties.secretKey())
			.build();
	}

}
