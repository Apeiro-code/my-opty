package com.myopty.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import com.myopty.order.config.DocumentProperties;
import com.myopty.order.exception.InvalidPrescriptionDocumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Upload rules for prescription documents. The declared content type is supplied
 * by the browser, so these tests pin the signature check that the shop relies on.
 */
class DocumentValidatorTest {

	private final DocumentValidator validator = new DocumentValidator(TestDocuments.properties());

	@Test
	void acceptsJpegScan() {
		MockMultipartFile file = new MockMultipartFile("document", "rx.jpg", "image/jpeg", TestDocuments.JPEG_BYTES);

		assertThat(this.validator.validate(file)).isEqualTo("image/jpeg");
	}

	@Test
	void acceptsPdfScan() {
		MockMultipartFile file = new MockMultipartFile("document", "rx.pdf", "application/pdf", TestDocuments.PDF_BYTES);

		assertThat(this.validator.validate(file)).isEqualTo("application/pdf");
	}

	@ParameterizedTest
	@CsvSource({ "image/jpeg, FF D8 FF", "image/png, 89 50 4E 47 0D 0A 1A 0A" })
	void detectsSupportedSignatures(String expected, String signature) {
		byte[] bytes = new byte[12];
		String[] octets = signature.split(" ");
		for (int i = 0; i < octets.length; i++) {
			bytes[i] = (byte) Integer.parseInt(octets[i], 16);
		}

		assertThat(DocumentValidator.detectContentType(bytes)).isEqualTo(expected);
	}

	@Test
	void detectsWebpFromRiffContainer() {
		byte[] bytes = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);

		assertThat(DocumentValidator.detectContentType(bytes)).isEqualTo("image/webp");
	}

	@Test
	void detectsHeicFromFtypBrand() {
		byte[] bytes = { 0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c' };

		assertThat(DocumentValidator.detectContentType(bytes)).isEqualTo("image/heic");
	}

	@Test
	void rejectsMissingDocument() {
		assertThatThrownBy(() -> this.validator.validate(null)).isInstanceOf(InvalidPrescriptionDocumentException.class)
			.hasMessageContaining("required");
	}

	@Test
	void rejectsEmptyDocument() {
		MockMultipartFile empty = new MockMultipartFile("document", "rx.jpg", "image/jpeg", new byte[0]);

		assertThatThrownBy(() -> this.validator.validate(empty)).isInstanceOf(InvalidPrescriptionDocumentException.class);
	}

	@Test
	void rejectsDocumentOverTheSizeLimit() {
		DocumentValidator strict = new DocumentValidator(new DocumentProperties(4, TestDocuments.properties().contentTypes()));
		MockMultipartFile file = new MockMultipartFile("document", "rx.jpg", "image/jpeg", TestDocuments.JPEG_BYTES);

		assertThatThrownBy(() -> strict.validate(file)).isInstanceOf(InvalidPrescriptionDocumentException.class)
			.hasMessageContaining("smaller than");
	}

	/**
	 * The customer can rename anything to {@code .jpg}; only the bytes decide.
	 */
	@Test
	void rejectsExecutableRenamedAsImage() {
		MockMultipartFile file = new MockMultipartFile("document", "virus.jpg", "image/jpeg",
				TestDocuments.EXECUTABLE_BYTES);

		assertThatThrownBy(() -> this.validator.validate(file)).isInstanceOf(InvalidPrescriptionDocumentException.class)
			.hasMessageContaining("PDF, JPEG, PNG, WebP or HEIC");
	}

	@Test
	void rejectsDeclaredTypeThatContradictsTheContent() {
		MockMultipartFile file = new MockMultipartFile("document", "rx.png", "image/png", TestDocuments.JPEG_BYTES);

		assertThatThrownBy(() -> this.validator.validate(file)).isInstanceOf(InvalidPrescriptionDocumentException.class)
			.hasMessageContaining("does not match");
	}

	@Test
	void acceptsGenericDeclaredTypeFromBrowsers() {
		MockMultipartFile file = new MockMultipartFile("document", "rx.jpg", "application/octet-stream",
				TestDocuments.JPEG_BYTES);

		assertThat(this.validator.validate(file)).isEqualTo("image/jpeg");
	}

}
