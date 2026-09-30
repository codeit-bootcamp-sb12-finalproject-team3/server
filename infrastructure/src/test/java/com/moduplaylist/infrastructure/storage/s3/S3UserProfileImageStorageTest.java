package com.moduplaylist.infrastructure.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3UserProfileImageStorageTest {

	private static final String BUCKET = "bucket";
	private static final String REGION = "ap-northeast-2";

	@Mock
	private S3Client s3Client;

	private S3UserProfileImageStorage storage;

	@BeforeEach
	void setUp() {
		storage = new S3UserProfileImageStorage(s3Client);
		ReflectionTestUtils.setField(storage, "bucket", BUCKET);
		ReflectionTestUtils.setField(storage, "region", REGION);
	}

	@Test
	void upload_usesUserSpecificNamespace() {
		UUID userId = UUID.randomUUID();

		String url = storage.upload(userId, new byte[] {1}, "image/png");

		ArgumentCaptor<PutObjectRequest> requestCaptor =
			ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
		assertThat(requestCaptor.getValue().key())
			.startsWith("profile-images/" + userId + "/")
			.endsWith(".png");
		assertThat(url).contains("/profile-images/" + userId + "/");
	}

	@Test
	void delete_deletesImageOwnedByUser() {
		UUID userId = UUID.randomUUID();
		String objectKey = "profile-images/" + userId + "/image.png";

		storage.delete(userId, s3Url(objectKey));

		ArgumentCaptor<DeleteObjectRequest> requestCaptor =
			ArgumentCaptor.forClass(DeleteObjectRequest.class);
		verify(s3Client).deleteObject(requestCaptor.capture());
		assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET);
		assertThat(requestCaptor.getValue().key()).isEqualTo(objectKey);
	}

	@Test
	void delete_doesNotDeleteAnotherUsersImage() {
		UUID userId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();

		storage.delete(userId, s3Url("profile-images/" + otherUserId + "/image.png"));

		verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	void delete_doesNotDeleteExternalUrl() {
		storage.delete(UUID.randomUUID(), "https://cdn.example.com/profile/image.png");

		verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	void delete_doesNotDeleteContentThumbnail() {
		storage.delete(UUID.randomUUID(), s3Url("content-thumbnails/image.png"));

		verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
	}

	private static String s3Url(String objectKey) {
		return "https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/" + objectKey;
	}
}
