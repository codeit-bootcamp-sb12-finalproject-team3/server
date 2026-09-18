package com.moduplaylist.core.content.entity;

import com.moduplaylist.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "episodes",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uq_episodes_external",
			columnNames = {"external_source", "external_id"}
		),
		@UniqueConstraint(
			name = "uq_episodes_season_number",
			columnNames = {"season_id", "episode_number"}
		)
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Episode extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "season_id", nullable = false, updatable = false)
	private Content season;

	@Column(name = "episode_number", nullable = false)
	private Integer episodeNumber;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "thumbnail_url", length = 500)
	private String thumbnailUrl;

	private Integer runtime;

	@Column(name = "external_source", length = 30)
	private String externalSource;

	@Column(name = "external_id")
	private Integer externalId;

	@Builder
	private Episode(
		Content season,
		Integer episodeNumber,
		String title,
		String description,
		String thumbnailUrl,
		Integer runtime,
		String externalSource,
		Integer externalId
	) {
		this.season = season;
		this.episodeNumber = episodeNumber;
		this.title = title;
		this.description = description;
		this.thumbnailUrl = thumbnailUrl;
		this.runtime = runtime;
		this.externalSource = externalSource;
		this.externalId = externalId;
		validate();
	}

	public void updateDetails(
		Integer episodeNumber,
		String title,
		String description,
		String thumbnailUrl,
		Integer runtime
	) {
		validateDetails(episodeNumber, title, thumbnailUrl, runtime);
		this.episodeNumber = episodeNumber;
		this.title = title;
		this.description = description;
		this.thumbnailUrl = thumbnailUrl;
		this.runtime = runtime;
	}

	private void validate() {
		if (season == null || season.getType() != ContentType.TV_SEASON) {
			throw new IllegalArgumentException("회차는 TV 시즌에 속해야 합니다.");
		}
		validateDetails(episodeNumber, title, thumbnailUrl, runtime);
		if ((externalSource == null) != (externalId == null)) {
			throw new IllegalArgumentException("외부 데이터 출처와 외부 ID는 함께 지정해야 합니다.");
		}
		if (externalSource != null
			&& (externalSource.isBlank() || externalSource.length() > 30)) {
			throw new IllegalArgumentException("외부 데이터 출처는 30자 이하의 문자열이어야 합니다.");
		}
	}

	private static void validateDetails(
		Integer episodeNumber,
		String title,
		String thumbnailUrl,
		Integer runtime
	) {
		if (episodeNumber == null || episodeNumber < 0) {
			throw new IllegalArgumentException("회차 번호는 0 이상이어야 합니다.");
		}
		if (title == null || title.isBlank() || title.length() > 255) {
			throw new IllegalArgumentException("회차 제목은 필수이며 255자 이하여야 합니다.");
		}
		if (runtime != null && runtime <= 0) {
			throw new IllegalArgumentException("회차 상영 시간은 0보다 커야 합니다.");
		}
		if (thumbnailUrl != null && (thumbnailUrl.isBlank() || thumbnailUrl.length() > 500)) {
			throw new IllegalArgumentException("회차 썸네일 URL은 500자 이하이거나 null이어야 합니다.");
		}
	}
}
