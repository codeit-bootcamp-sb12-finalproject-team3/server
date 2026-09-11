package com.moduplaylist.core.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(
	name = "content_ott",
	indexes = {
		@Index(name = "idx_content_ott_platform", columnList = "ott_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uq_content_ott",
			columnNames = {"content_id", "ott_id"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentOtt extends ContentUuidEntity {

	private static final int MAX_WATCH_URL_LENGTH = 1000;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "content_id",
		nullable = false,
		updatable = false
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Content content;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "ott_id",
		nullable = false,
		updatable = false
	)
	private OttPlatform ottPlatform;

	@Column(name = "watch_url", length = MAX_WATCH_URL_LENGTH)
	private String watchUrl;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	private ContentOtt(
		Content content,
		OttPlatform ottPlatform,
		String watchUrl
	) {
		this.content = Objects.requireNonNull(content, "content는 필수입니다.");
		this.ottPlatform = Objects.requireNonNull(
			ottPlatform,
			"ottPlatform은 필수입니다."
		);
		this.watchUrl = normalizeWatchUrl(watchUrl);
	}

	public static ContentOtt create(
		Content content,
		OttPlatform ottPlatform,
		String watchUrl
	) {
		return new ContentOtt(content, ottPlatform, watchUrl);
	}

	public void updateWatchUrl(String watchUrl) {
		this.watchUrl = normalizeWatchUrl(watchUrl);
	}

	private static String normalizeWatchUrl(String watchUrl) {
		if (watchUrl == null) {
			return null;
		}

		String normalized = watchUrl.strip();

		if (normalized.isEmpty()) {
			return null;
		}

		if (normalized.length() > MAX_WATCH_URL_LENGTH) {
			throw new IllegalArgumentException(
				"watchUrl은 1000자를 초과할 수 없습니다."
			);
		}

		return normalized;
	}
}
