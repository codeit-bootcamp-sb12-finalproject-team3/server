package com.moduplaylist.core.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Entity
@Table(
	name = "ott_platforms",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uq_ott_platforms_name",
			columnNames = "name"
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OttPlatform extends ContentUuidEntity {

	private static final int MAX_NAME_LENGTH = 50;
	private static final int MAX_LOGO_URL_LENGTH = 500;

	@Column(
		name = "name",
		nullable = false,
		updatable = false,
		length = MAX_NAME_LENGTH
	)
	private String name;

	@Column(name = "logo_url", length = MAX_LOGO_URL_LENGTH)
	private String logoUrl;

	@CreationTimestamp
	@Column(
		name = "created_at",
		nullable = false,
		updatable = false
	)
	private Instant createdAt;

	private OttPlatform(String name, String logoUrl) {
		this.name = normalizeName(name);
		this.logoUrl = normalizeLogoUrl(logoUrl);
	}

	public static OttPlatform create(String name, String logoUrl) {
		return new OttPlatform(name, logoUrl);
	}

	public void updateLogoUrl(String logoUrl) {
		this.logoUrl = normalizeLogoUrl(logoUrl);
	}

	private static String normalizeName(String name) {
		if (name == null) {
			throw new IllegalArgumentException("name은 필수입니다.");
		}

		String normalized = name.strip();

		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("name은 비어 있을 수 없습니다.");
		}

		if (normalized.length() > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException(
				"name은 " + MAX_NAME_LENGTH + "자를 초과할 수 없습니다."
			);
		}

		return normalized;
	}

	private static String normalizeLogoUrl(String logoUrl) {
		if (logoUrl == null) {
			return null;
		}

		String normalized = logoUrl.strip();

		if (normalized.isEmpty()) {
			return null;
		}

		if (normalized.length() > MAX_LOGO_URL_LENGTH) {
			throw new IllegalArgumentException(
				"logoUrl은 " + MAX_LOGO_URL_LENGTH + "자를 초과할 수 없습니다."
			);
		}

		return normalized;
	}
}
