package com.moduplaylist.core.content.entity;

import com.moduplaylist.core.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
	name = "episodes",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uq_episodes_external_id",
			columnNames = "external_id"
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
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Content season;

	@Column(name = "episode_number", nullable = false)
	private Integer episodeNumber;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "still_image_url", length = 500)
	private String stillImageUrl;

	private Integer runtime;

	@Column(name = "air_date")
	private LocalDate airDate;

	@Column(name = "external_id", nullable = false, updatable = false)
	private Integer externalId;

	@Builder
	private Episode(
		Content season,
		Integer episodeNumber,
		String title,
		String description,
		String stillImageUrl,
		Integer runtime,
		LocalDate airDate,
		Integer externalId
	) {
		this.season = season;
		this.episodeNumber = episodeNumber;
		this.title = title;
		this.description = description;
		this.stillImageUrl = stillImageUrl;
		this.runtime = runtime;
		this.airDate = airDate;
		this.externalId = externalId;
		validate();
	}

	public void updateDetails(
		Integer episodeNumber,
		String title,
		String description,
		String stillImageUrl,
		Integer runtime,
		LocalDate airDate
	) {
		validateDetails(episodeNumber, title, stillImageUrl, runtime);
		this.episodeNumber = episodeNumber;
		this.title = title;
		this.description = description;
		this.stillImageUrl = stillImageUrl;
		this.runtime = runtime;
		this.airDate = airDate;
	}

	private void validate() {
		if (season == null || season.getType() != ContentType.TV_SEASON) {
			throw new IllegalArgumentException("회차는 TV 시즌에 속해야 합니다.");
		}
		validateDetails(episodeNumber, title, stillImageUrl, runtime);
		if (externalId == null) {
			throw new IllegalArgumentException("회차 외부 ID는 필수입니다.");
		}
	}

	private static void validateDetails(
		Integer episodeNumber,
		String title,
		String stillImageUrl,
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
		if (stillImageUrl != null && (stillImageUrl.isBlank() || stillImageUrl.length() > 500)) {
			throw new IllegalArgumentException("회차 스틸 이미지 URL은 500자 이하이거나 null이어야 합니다.");
		}
	}
}
