package com.myopty.order.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.myopty.order.exception.InvalidPrescriptionDocumentException;
import com.myopty.order.service.DocumentValidator;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Wiring of the document-storage configuration. The whole application context
 * cannot start in this environment (it needs MySQL), so the module's own
 * properties are bound and its beans assembled in isolation: a rename of a
 * property in {@code application.yaml} has to be caught here rather than at
 * runtime on a first upload.
 */
class DocumentStorageConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withUserConfiguration(DocumentPropertiesConfiguration.class, MinioClientConfig.class, DocumentValidator.class)
		.withPropertyValues("myopty.documents.max-size-bytes=2097152",
				"myopty.documents.content-types[0]=image/jpeg", "myopty.documents.content-types[1]=application/pdf",
				"myopty.documents.storage.minio.endpoint=http://localhost:9000",
				"myopty.documents.storage.minio.access-key=minioadmin",
				"myopty.documents.storage.minio.secret-key=minioadmin",
				"myopty.documents.storage.minio.bucket=prescription-documents",
				"myopty.documents.storage.minio.auto-create-bucket=true");

	@Test
	void bindsTheDocumentRules() {
		this.runner.run(context -> {
			DocumentProperties properties = context.getBean(DocumentProperties.class);
			assertThat(properties.maxSizeBytes()).isEqualTo(2097152L);
			assertThat(properties.contentTypes()).containsExactly("image/jpeg", "application/pdf");
		});
	}

	@Test
	void bindsTheStoreConnection() {
		this.runner.run(context -> {
			MinioProperties properties = context.getBean(MinioProperties.class);
			assertThat(properties.endpoint()).isEqualTo("http://localhost:9000");
			assertThat(properties.accessKey()).isEqualTo("minioadmin");
			assertThat(properties.secretKey()).isEqualTo("minioadmin");
			assertThat(properties.bucket()).isEqualTo("prescription-documents");
			assertThat(properties.autoCreateBucket()).isTrue();
		});
	}

	@Test
	void buildsTheObjectStorageClientFromConfiguration() {
		this.runner.run(context -> {
			assertThat(context).hasSingleBean(MinioClient.class);
			assertThat(context.getBean(MinioClient.class)).isNotNull();
		});
	}

	/**
	 * The rules the shop configured have to be the rules the validator enforces, so
	 * the limits are checked by behaviour rather than by reading the record back.
	 */
	@Test
	void givesTheValidatorTheConfiguredLimits() {
		this.runner.run(context -> {
			DocumentValidator validator = context.getBean(DocumentValidator.class);

			assertThat(validator.validate(jpeg(1024))).isEqualTo("image/jpeg");
			assertThatThrownBy(() -> validator.validate(jpeg(3 * 1024 * 1024))).isInstanceOf(
					InvalidPrescriptionDocumentException.class)
				.hasMessageContaining("2 MB");
		});
	}

	@Test
	void rejectsAMediaTypeTheShopDoesNotAccept() {
		this.runner.run(context -> {
			DocumentValidator validator = context.getBean(DocumentValidator.class);
			byte[] png = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D };

			assertThatThrownBy(
					() -> validator.validate(new MockMultipartFile("document", "rx.png", "image/png", png)))
				.isInstanceOf(InvalidPrescriptionDocumentException.class);
		});
	}

	private static MockMultipartFile jpeg(int size) {
		byte[] jpeg = new byte[size];
		byte[] signature = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46,
				0x00, 0x01 };
		System.arraycopy(signature, 0, jpeg, 0, Math.min(signature.length, size));
		return new MockMultipartFile("document", "rx.jpg", "image/jpeg", jpeg);
	}

	/**
	 * {@link DocumentProperties} is bound to a record, so an absent list has to
	 * normalise instead of leaving a null that the validator would dereference.
	 */
	@Test
	void toleratesAnEmptyContentTypeList() {
		this.runner.withPropertyValues("myopty.documents.content-types=").run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(DocumentProperties.class).contentTypes()).isEqualTo(List.of());
		});
	}

	@Test
	void refusesToBuildAClientWithoutAnEndpoint() {
		this.runner.withPropertyValues("myopty.documents.storage.minio.endpoint=").run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasMessageContaining("endpoint");
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({ DocumentProperties.class, MinioProperties.class })
	static class DocumentPropertiesConfiguration {

	}

}
