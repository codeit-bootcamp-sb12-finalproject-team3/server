package com.moduplaylist.core.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;
import java.util.Objects;

@Getter
@Entity
@Table(
	name = "content_platforms",
	indexes = {
		@Index(
			name = "idx_content_platforms_lookup",
			columnList = "content_id, region_code, platform_id"
		),
		@Index(
			name = "idx_content_platforms_platform",
			columnList = "platform_id"
		)
	},
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uq_content_platforms",
			columnNames = {"content_id", "platform_id", "region_code"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentPlatform extends ContentUuidEntity {

	private static final int MAX_URL_LENGTH = 1000;
	private static final String DEFAULT_REGION_CODE = "KR";

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "content_id",
		nullable = false,
		updatable = false
	)
	private Content content;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "platform_id",
		nullable = false,
		updatable = false
	)
	private Platform platform;

	@Enumerated(EnumType.STRING)
	@Column(
		name = "source",
		nullable = false,
		updatable = false,
		columnDefinition = "enum('TMDB','MANUAL')"
	)
	private PlatformSource source;

	@Column(name = "region_code", nullable = false, length = 2)
	private String regionCode;

	@Column(name = "url", nullable = false, length = MAX_URL_LENGTH)
	private String url;

	private ContentPlatform(
		Content content,
		Platform platform,
		PlatformSource source,
		String regionCode,
		String url
	) {
		this.content = Objects.requireNonNull(content, "content는 필수입니다.");
		this.platform = Objects.requireNonNull(
			platform,
			"platform은 필수입니다."
		);
		this.source = Objects.requireNonNull(source, "source는 필수입니다.");
		this.regionCode = normalizeRegionCode(regionCode);
		this.url = normalizeUrl(url);
	}

	public static ContentPlatform create(
		Content content,
		Platform platform,
		PlatformSource source,
		String regionCode,
		String url
	) {
		return new ContentPlatform(content, platform, source, regionCode, url);
	}

	public void updateUrl(String url) {
		this.url = normalizeUrl(url);
	}

	public void updateRegionCode(String regionCode) {
		this.regionCode = normalizeRegionCode(regionCode);
	}

	private static String normalizeRegionCode(String regionCode) {
		if (regionCode == null) {
			return DEFAULT_REGION_CODE;
		}

		String normalized = regionCode.strip();
		if (normalized.length() != 2
			|| !normalized.equals(normalized.toUpperCase(Locale.ROOT))) {
			throw new IllegalArgumentException("regionCode는 대문자 2자리여야 합니다.");
		}
		return normalized;
	}

	private static String normalizeUrl(String url) {
		if (url == null) {
			throw new IllegalArgumentException("url은 필수입니다.");
		}

		String normalized = url.strip();
		if (normalized.length() > MAX_URL_LENGTH
			|| (!normalized.startsWith("http://") && !normalized.startsWith("https://"))) {
			throw new IllegalArgumentException("url은 1000자 이하의 HTTP(S) URL이어야 합니다.");
		}
		return normalized;
	}

	public enum PlatformSource {
		TMDB,
		MANUAL
	}
}
