package com.myopty.order.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.myopty.order.config.MinioProperties;
import com.myopty.order.domain.PrescriptionDocument;
import com.myopty.order.exception.DocumentStorageException;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The MinIO adapter, which is the only part of the module that cannot be covered
 * without a running object store. The client is mocked, so what is asserted here
 * is the module's own behaviour: the bucket it configures, the key it writes to,
 * the metadata it echoes back and the errors it translates. The wire protocol
 * itself still needs a real MinIO.
 */
@ExtendWith(MockitoExtension.class)
class MinioDocumentStorageTest {

	private static final String BUCKET = "prescriptions";

	private static final String OBJECT_KEY = "prescriptions/2026/09/1f0c6b1a.pdf";

	@Mock
	private MinioClient client;

	@Captor
	private ArgumentCaptor<PutObjectArgs> putObjectArgs;

	@Captor
	private ArgumentCaptor<StatObjectArgs> statObjectArgs;

	private MinioDocumentStorage storage;

	@BeforeEach
	void setUp() {
		this.storage = new MinioDocumentStorage(this.client,
				new MinioProperties("http://localhost:9000", "minioadmin", "minioadmin", BUCKET, true));
	}

	@Test
	void createsTheBucketOnFirstStartWhenItIsMissing() throws Exception {
		when(this.client.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

		this.storage.ensureBucket();

		ArgumentCaptor<MakeBucketArgs> args = ArgumentCaptor.forClass(MakeBucketArgs.class);
		verify(this.client).makeBucket(args.capture());
		assertThat(args.getValue().bucket()).isEqualTo(BUCKET);
	}

	@Test
	void leavesAnExistingBucketAlone() throws Exception {
		when(this.client.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

		this.storage.ensureBucket();

		verify(this.client, never()).makeBucket(any(MakeBucketArgs.class));
	}

	@Test
	void doesNotCallTheStoreWhenBucketCreationIsDisabled() {
		MinioDocumentStorage manual = new MinioDocumentStorage(this.client,
				new MinioProperties("http://localhost:9000", "minioadmin", "minioadmin", BUCKET, false));

		manual.ensureBucket();

		verifyNoInteractions(this.client);
	}

	/**
	 * A store that is not up yet must not stop the application from booting; the
	 * failure has to surface on the first upload instead.
	 */
	@Test
	void stillBootsWhenTheStoreIsUnreachable() throws Exception {
		when(this.client.bucketExists(any(BucketExistsArgs.class))).thenThrow(new IOException("connection refused"));

		assertThatCode(() -> this.storage.ensureBucket()).doesNotThrowAnyException();
	}

	@Test
	void storeWritesTheBytesUnderTheGivenKey() throws Exception {
		InputStream content = new ByteArrayInputStream("%PDF-1.7".getBytes(StandardCharsets.UTF_8));

		PrescriptionDocument stored = this.storage.store(OBJECT_KEY, content, 8L, "scan.pdf", "application/pdf");

		verify(this.client).putObject(this.putObjectArgs.capture());
		assertThat(this.putObjectArgs.getValue().bucket()).isEqualTo(BUCKET);
		assertThat(this.putObjectArgs.getValue().object()).isEqualTo(OBJECT_KEY);
		assertThat(this.putObjectArgs.getValue().contentType()).isEqualTo("application/pdf");
		assertThat(stored).isEqualTo(new PrescriptionDocument(OBJECT_KEY, "scan.pdf", "application/pdf", 8L, null));
	}

	@Test
	void storeTranslatesARejectedWrite() throws Exception {
		InputStream content = new ByteArrayInputStream(new byte[] { 1 });
		doThrow(new IOException("bucket is read only"))
			.when(this.client)
			.putObject(any(PutObjectArgs.class));

		assertThatThrownBy(() -> this.storage.store(OBJECT_KEY, content, 1L, "scan.pdf", "application/pdf"))
			.isInstanceOf(DocumentStorageException.class)
			.hasMessageContaining("Could not store");
	}

	@Test
	void openReportsTheMetadataRecordedByTheStore() throws Exception {
		StatObjectResponse stat = mock(StatObjectResponse.class);
		GetObjectResponse body = mock(GetObjectResponse.class);
		when(this.client.statObject(any(StatObjectArgs.class))).thenReturn(stat);
		when(this.client.getObject(any(GetObjectArgs.class))).thenReturn(body);
		when(stat.contentType()).thenReturn("image/jpeg");
		when(stat.size()).thenReturn(2048L);

		DocumentStorage.DocumentContent content = this.storage.open(OBJECT_KEY);

		verify(this.client).statObject(this.statObjectArgs.capture());
		assertThat(this.statObjectArgs.getValue().bucket()).isEqualTo(BUCKET);
		assertThat(this.statObjectArgs.getValue().object()).isEqualTo(OBJECT_KEY);
		assertThat(content.contentType()).isEqualTo("image/jpeg");
		assertThat(content.sizeBytes()).isEqualTo(2048L);
		assertThat(content.stream()).isSameAs(body);
	}

	@Test
	void openFallsBackToOctetStreamWhenTheStoreReportsNoContentType() throws Exception {
		StatObjectResponse stat = mock(StatObjectResponse.class);
		when(this.client.statObject(any(StatObjectArgs.class))).thenReturn(stat);
		when(this.client.getObject(any(GetObjectArgs.class)))
			.thenReturn(mock(GetObjectResponse.class));
		when(stat.contentType()).thenReturn(null);

		assertThat(this.storage.open(OBJECT_KEY).contentType()).isEqualTo("application/octet-stream");
	}

	@Test
	void openTranslatesAMissingObject() throws Exception {
		when(this.client.statObject(any(StatObjectArgs.class))).thenThrow(new IOException("no such key"));

		assertThatThrownBy(() -> this.storage.open(OBJECT_KEY)).isInstanceOf(DocumentStorageException.class)
			.hasMessageContaining("could not be read");
	}

	@Test
	void deleteRemovesTheObject() throws Exception {
		this.storage.delete(OBJECT_KEY);

		ArgumentCaptor<RemoveObjectArgs> args = ArgumentCaptor.forClass(RemoveObjectArgs.class);
		verify(this.client).removeObject(args.capture());
		assertThat(args.getValue().object()).isEqualTo(OBJECT_KEY);
	}

	/**
	 * Delete only ever runs as compensation for a failed database write, so it must
	 * not turn that failure into a second one.
	 */
	@Test
	void deleteNeverFailsTheRequest() throws Exception {
		doThrow(new IOException("already gone")).when(this.client).removeObject(any(RemoveObjectArgs.class));

		assertThatCode(() -> this.storage.delete(OBJECT_KEY)).doesNotThrowAnyException();
	}

}
