package com.moduplaylist.core.content.entity;

import com.moduplaylist.core.common.BaseEntity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseEntity {
	private static final BigDecimal MAX_RATING = new BigDecimal("5.00");
	private static final int RATING_SCALE = 2;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_content_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Content parentContent;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(name = "season_number")
	private Integer seasonNumber;

	@Column(name = "season_count")
	private Integer seasonCount;

	@Column(name = "episode_count")
	private Integer episodeCount;

	@Convert(converter = ContentTypeConverter.class)
	@Column(nullable = false, columnDefinition = "enum('movie','tvSeries','tvSeason','sport')")
	private ContentType type;

	@Column(name = "sport_type", length = 50)
	private String sportType;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "thumbnail_url", length = 500)
	private String thumbnailUrl;

	@Column(name = "release_date")
	private LocalDate releaseDate;

	private Integer runtime;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private Map<String, Object> metadata;

	@Column(name = "external_source", length = 30)
	private String externalSource;

	@Column(name = "external_id")
	private Integer externalId;

	@Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
	private BigDecimal averageRating = new BigDecimal("0.00");

	@Column(name = "like_count", nullable = false, columnDefinition = "INT UNSIGNED")
	private long likeCount = 0;

	@Column(name = "review_count", nullable = false, columnDefinition = "INT UNSIGNED")
	private long reviewCount = 0;

	@Builder
	private Content(
		Content parentContent,
		String title,
		Integer seasonNumber,
		Integer seasonCount,
		Integer episodeCount,
		ContentType type,
		String sportType,
		String description,
		String thumbnailUrl,
		LocalDate releaseDate,
		Integer runtime,
		Map<String, Object> metadata,
		String externalSource,
		Integer externalId
	) {
		this.parentContent = parentContent;
		this.title = title;
		this.seasonNumber = seasonNumber;
		this.seasonCount = seasonCount;
		this.episodeCount = episodeCount;
		this.type = type;
		this.sportType = sportType;
		this.description = description;
		this.thumbnailUrl = thumbnailUrl;
		this.releaseDate = releaseDate;
		this.runtime = runtime;
		this.metadata = metadata;
		this.externalSource = externalSource;
		this.externalId = externalId;
		validate();
	}

	public void updateDetails(String title, String description, String thumbnailUrl) {
		if (title != null) {
			validateTitle(title);
			this.title = title;
		}
		if (description != null) this.description = description;
		if (thumbnailUrl != null) this.thumbnailUrl = thumbnailUrl;
	}

	public void updateType(
		ContentType type,
		Content parentContent,
		Integer seasonNumber,
		Integer seasonCount,
		Integer episodeCount,
		String sportType
	) {
		validateTypeStructure(
			type,
			parentContent,
			seasonNumber,
			seasonCount,
			episodeCount,
			sportType
		);

		this.type = type;
		this.parentContent = parentContent;
		this.seasonNumber = seasonNumber;
		this.seasonCount = seasonCount;
		this.episodeCount = episodeCount;
		this.sportType = sportType;
	}

	public void updateReviewStatistics(BigDecimal averageRating, long reviewCount) {
		if (averageRating == null || averageRating.signum() < 0
			|| averageRating.compareTo(MAX_RATING) > 0) {
			throw new IllegalArgumentException("평점은 0에서 5 사이여야 합니다.");
		}
		if (reviewCount < 0) {
			throw new IllegalArgumentException("리뷰 수는 음수일 수 없습니다.");
		}
		this.averageRating = averageRating.setScale(RATING_SCALE, RoundingMode.HALF_UP);
		this.reviewCount = reviewCount;
	}

	private void validate() {
		validateTitle(title);
		validateTypeStructure(
			type,
			parentContent,
			seasonNumber,
			seasonCount,
			episodeCount,
			sportType
		);
		if (runtime != null && runtime <= 0) {
			throw new IllegalArgumentException("상영 시간은 0보다 커야 합니다.");
		}
		if ((externalSource == null) != (externalId == null)) {
			throw new IllegalArgumentException("외부 데이터 출처와 외부 ID는 함께 지정해야 합니다.");
		}
	}

	private static void validateTypeStructure(
		ContentType type,
		Content parentContent,
		Integer seasonNumber,
		Integer seasonCount,
		Integer episodeCount,
		String sportType
	) {
		if (type == null) throw new IllegalArgumentException("콘텐츠 타입은 필수입니다.");
		if (type == ContentType.TV_SEASON) {
			if (parentContent == null || parentContent.getType() != ContentType.TV_SERIES
				|| seasonNumber == null) {
				throw new IllegalArgumentException("시즌은 TV 시리즈 부모와 시즌 번호가 필요합니다.");
			}
		} else if (parentContent != null || seasonNumber != null || episodeCount != null) {
			throw new IllegalArgumentException("부모 콘텐츠, 시즌 번호, 회차 수는 시즌에만 지정할 수 있습니다.");
		}
		if (type != ContentType.TV_SERIES && seasonCount != null) {
			throw new IllegalArgumentException("전체 시즌 수는 TV 시리즈에만 지정할 수 있습니다.");
		}
		if ((type == ContentType.SPORT && (sportType == null || sportType.isBlank()))
			|| (type != ContentType.SPORT && sportType != null)) {
			throw new IllegalArgumentException("스포츠 콘텐츠에만 종목을 필수로 지정해야 합니다.");
		}
		if (seasonNumber != null && seasonNumber < 0) {
			throw new IllegalArgumentException("시즌 번호는 0 이상이어야 합니다.");
		}
		if (seasonCount != null && seasonCount < 0) {
			throw new IllegalArgumentException("전체 시즌 수는 음수일 수 없습니다.");
		}
		if (episodeCount != null && episodeCount < 0) {
			throw new IllegalArgumentException("회차 수는 음수일 수 없습니다.");
		}
	}

	private void validateTitle(String title) {
		if (title == null || title.isBlank() || title.length() > 255) {
			throw new IllegalArgumentException("콘텐츠 제목은 필수이며 255자 이하여야 합니다.");
		}
	}
}
