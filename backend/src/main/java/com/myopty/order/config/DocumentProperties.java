package com.myopty.order.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Upload rules for prescription documents, bound from {@code myopty.documents.*}.
 *
 * @param maxSizeBytes largest accepted document
 * @param contentTypes media types the shop accepts, checked against the sniffed
 *                     file signature rather than the client-declared header
 */
@ConfigurationProperties(prefix = "myopty.documents")
public record DocumentProperties(long maxSizeBytes, List<String> contentTypes) {

	public DocumentProperties {
		contentTypes = contentTypes == null ? List.of() : List.copyOf(contentTypes);
	}

}
