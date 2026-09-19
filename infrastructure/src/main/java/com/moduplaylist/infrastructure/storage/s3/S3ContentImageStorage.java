package com.moduplaylist.infrastructure.storage.s3;

import com.moduplaylist.infrastructure.storage.ContentImageStorage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
public class S3ContentImageStorage implements ContentImageStorage {

	private final S3Client s3Client;

	@Value("${aws.s3.bucket}")
	private String bucket;

	@Value("${aws.s3.region}")
	private String region;

	@Override
	public String upload(String objectKey, byte[] data, String contentType) {
		if (bucket == null || bucket.isBlank()) {
			throw new IllegalStateException("S3 bucket이 구성되지 않았습니다.");
		}
		s3Client.putObject(
			PutObjectRequest.builder()
				.bucket(bucket)
				.key(objectKey)
				.contentType(contentType)
				.build(),
			RequestBody.fromBytes(data)
		);
		return "https://" + bucket + ".s3." + region + ".amazonaws.com/"
			+ URLEncoder.encode(objectKey, StandardCharsets.UTF_8).replace("%2F", "/");
	}

	@Override
	public void delete(String objectKey) {
		s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
	}
}
