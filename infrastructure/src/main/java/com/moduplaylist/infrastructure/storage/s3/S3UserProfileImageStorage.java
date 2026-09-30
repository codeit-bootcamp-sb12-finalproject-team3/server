package com.moduplaylist.infrastructure.storage.s3;

import com.moduplaylist.infrastructure.storage.UserProfileImageStorage;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
public class S3UserProfileImageStorage implements UserProfileImageStorage {

	private static final String PROFILE_IMAGE_PREFIX = "profile-images/";

	private final S3Client s3Client;

	@Value("${aws.s3.bucket}")
	private String bucket;

	@Value("${aws.s3.region}")
	private String region;

	@Override
	public String upload(UUID userId, byte[] data, String contentType) {
		if (bucket == null || bucket.isBlank()) {
			throw new IllegalStateException("S3 bucket이 구성되지 않았습니다.");
		}
		String extension = switch (contentType) {
			case "image/jpeg" -> "jpg";
			case "image/png" -> "png";
			case "image/webp" -> "webp";
			default -> throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
		};
		String objectKey = userProfileImagePrefix(userId) + UUID.randomUUID() + "." + extension;
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
	public void delete(UUID userId, String url) {
		if (url == null || url.isBlank() || bucket == null || bucket.isBlank()
			|| region == null || region.isBlank()) {
			return;
		}
		try {
			URI uri = URI.create(url);
			String expectedHost = bucket + ".s3." + region + ".amazonaws.com";
			if (!"https".equalsIgnoreCase(uri.getScheme())
				|| uri.getHost() == null
				|| !expectedHost.equalsIgnoreCase(uri.getHost())
				|| uri.getPort() != -1
				|| uri.getUserInfo() != null
				|| uri.getQuery() != null
				|| uri.getFragment() != null
				|| uri.getRawPath() == null
				|| !uri.getRawPath().startsWith("/")) {
				return;
			}
			String objectKey = URLDecoder.decode(
				uri.getRawPath().substring(1), StandardCharsets.UTF_8);
			if (objectKey.startsWith(userProfileImagePrefix(userId))) {
				s3Client.deleteObject(
					DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
			}
		} catch (IllegalArgumentException ignored) {
			// 현재 저장소에서 생성한 URL이 아니면 삭제하지 않는다.
		}
	}

	private String userProfileImagePrefix(UUID userId) {
		return PROFILE_IMAGE_PREFIX + userId + "/";
	}
}
