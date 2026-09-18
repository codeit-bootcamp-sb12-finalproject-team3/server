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
import org.hibernate.type.SqlTypes;

@Entity
@Table(
	name = "contents",
	indexes = {
		@Index(name = "idx_contents_created", columnList = "hidden, created_at DESC, id DESC"),
		@Index(
			name = "idx_contents_rating",
			columnList = "hidden, average_rating DESC, review_count DESC, id DESC"
		),
		@Index(
			name = "idx_contents_type_created",
			columnList = "hidden, type, created_at DESC, id DESC"
		),
		@Index(
			name = "idx_contents_type_rating",
			columnList = "hidden, type, average_rating DESC, review_count DESC, id DESC"
		),
		@Index(
			name = "idx_contents_parent_season",
			columnList = "parent_content_id, hidden, season_number"
		)
	},
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uq_contents_external",
			columnNames = {"external_source", "type", "external_id"}
		),
		@UniqueConstraint(
			name = "uq_contents_parent_season",
			columnNames = {"parent_content_id", "season_number"}
		)
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseEntity {
	private static final BigDecimal MAX_RATING = new BigDecimal("5.00");
	private static final BigDecimal MIN_REVIEW_RATING = new BigDecimal("0.50");
	private static final int RATING_SCALE = 2;
	private static final int MAX_THUMBNAIL_URL_LENGTH = 500;
	private static final int MAX_EXTERNAL_SOURCE_LENGTH = 30;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_content_id")
	private Content parentContent;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(name = "season_number")
	private Integer seasonNumber;

	@Column(name = "episode_count")
	private Integer episodeCount;

	@Convert(converter = ContentTypeConverter.class)
	@Column(
		nullable = false,
		updatable = false,
		columnDefinition = "enum('movie','tvSeries','tvSeason','sport')"
	)
	private ContentType type;

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

	@Enumerated(EnumType.STRING)
	@Column(
		name = "ai_tagging_status",
		columnDefinition = "enum('PENDING','COMPLETED','COMPLETED_PARTIAL','FAILED')"
	)
	private AiTaggingStatus aiTaggingStatus;

	@Column(nullable = false)
	private boolean hidden = false;

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
		Integer episodeCount,
		ContentType type,
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
		this.episodeCount = episodeCount;
		this.type = type;
		this.description = description;
		this.thumbnailUrl = thumbnailUrl;
		this.releaseDate = releaseDate;
		this.runtime = runtime;
		this.metadata = metadata;
		this.externalSource = externalSource;
		this.externalId = externalId;
		validate();
	}

	public void updateCommonDetails(
		String title,
		String description,
		LocalDate releaseDate,
		Map<String, Object> metadata
	) {
		validateTitle(title);
		if (type == ContentType.SPORT
			&& (releaseDate != null || metadata != null)) {
			throw new IllegalArgumentException(
				"스포츠에는 공개일과 메타데이터를 저장할 수 없습니다."
			);
		}
		validateTvSeriesContainerFields(
			type,
			description,
			null,
			releaseDate,
			null,
			metadata
		);
		this.title = title;
		this.description = description;
		this.releaseDate = releaseDate;
		this.metadata = metadata;
	}

	public void replaceThumbnailUrl(String thumbnailUrl) {
		validateOptionalThumbnailUrl(thumbnailUrl);
		validateTvSeriesContainerFields(
			type,
			null,
			thumbnailUrl,
			null,
			null,
			null
		);
		this.thumbnailUrl = thumbnailUrl;
	}

	public void updateMovieDetails(Integer runtime) {
		validateCurrentType(ContentType.MOVIE);
		if (runtime != null && runtime <= 0) {
			throw new IllegalArgumentException("상영 시간은 0보다 커야 합니다.");
		}
		this.runtime = runtime;
	}

	public void updateTvSeasonDetails(
		Content parentContent,
		Integer seasonNumber,
		Integer episodeCount,
		Integer runtime
	) {
		validateCurrentType(ContentType.TV_SEASON);
		validateTypeStructure(
			type,
			parentContent,
			seasonNumber,
			episodeCount,
			runtime
		);
		this.parentContent = parentContent;
		this.seasonNumber = seasonNumber;
		this.episodeCount = episodeCount;
		this.runtime = runtime;
	}

	public void updateAiTaggingStatus(AiTaggingStatus aiTaggingStatus) {
		if (type != ContentType.MOVIE && type != ContentType.TV_SEASON) {
			throw new IllegalStateException(
				"AI 태깅 상태는 영화와 TV 시즌에만 설정할 수 있습니다."
			);
		}
		if (externalSource == null || externalId == null) {
			throw new IllegalStateException(
				"AI 태깅 상태는 외부 콘텐츠에만 설정할 수 있습니다."
			);
		}
		this.aiTaggingStatus = aiTaggingStatus;
	}

	public void hide() {
		this.hidden = true;
	}

	public void show() {
		this.hidden = false;
	}

	public boolean isReviewable() {
		return type != null && type.isReviewable();
	}

	public void updateReviewStatistics(BigDecimal averageRating, long reviewCount) {
		if (!isReviewable()) {
			throw new IllegalStateException("TV 시리즈는 리뷰와 평점 집계 대상이 아닙니다.");
		}
		if (averageRating == null || averageRating.signum() < 0
			|| averageRating.compareTo(MAX_RATING) > 0) {
			throw new IllegalArgumentException("평점은 0에서 5 사이여야 합니다.");
		}
		if (reviewCount < 0) {
			throw new IllegalArgumentException("리뷰 수는 음수일 수 없습니다.");
		}
		if ((reviewCount == 0 && averageRating.signum() != 0)
			|| (reviewCount > 0 && averageRating.compareTo(MIN_REVIEW_RATING) < 0)) {
			throw new IllegalArgumentException("평균 평점과 리뷰 수가 일치하지 않습니다.");
		}
		this.averageRating = averageRating.setScale(RATING_SCALE, RoundingMode.HALF_UP);
		this.reviewCount = reviewCount;
	}

	private void validateCurrentType(ContentType expectedType) {
		if (type != expectedType) {
			throw new IllegalStateException("콘텐츠 타입에 맞지 않는 수정입니다.");
		}
	}

	private void validate() {
		validateTitle(title);
		validateOptionalThumbnailUrl(thumbnailUrl);
		validateTypeStructure(
			type,
			parentContent,
			seasonNumber,
			episodeCount,
			runtime
		);
		validateTvSeriesContainerFields(
			type,
			description,
			thumbnailUrl,
			releaseDate,
			runtime,
			metadata
		);
		if ((externalSource == null) != (externalId == null)) {
			throw new IllegalArgumentException("외부 데이터 출처와 외부 ID는 함께 지정해야 합니다.");
		}
		if (externalSource != null
			&& (externalSource.isBlank() || externalSource.length() > MAX_EXTERNAL_SOURCE_LENGTH)) {
			throw new IllegalArgumentException("외부 데이터 출처는 30자 이하의 문자열이어야 합니다.");
		}
	}

	private static void validateTypeStructure(
		ContentType type,
		Content parentContent,
		Integer seasonNumber,
		Integer episodeCount,
		Integer runtime
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
		if (runtime != null && runtime <= 0) {
			throw new IllegalArgumentException("상영 시간은 0보다 커야 합니다.");
		}
		if (seasonNumber != null && seasonNumber < 0) {
			throw new IllegalArgumentException("시즌 번호는 0 이상이어야 합니다.");
		}
		if (episodeCount != null && episodeCount < 0) {
			throw new IllegalArgumentException("회차 수는 음수일 수 없습니다.");
		}
	}

	private static void validateTvSeriesContainerFields(
		ContentType type,
		String description,
		String thumbnailUrl,
		LocalDate releaseDate,
		Integer runtime,
		Map<String, Object> metadata
	) {
		if (type == ContentType.TV_SERIES
			&& (description != null
				|| thumbnailUrl != null
				|| releaseDate != null
				|| runtime != null
				|| metadata != null)) {
			throw new IllegalArgumentException(
				"TV 시리즈 컨테이너에는 설명, 썸네일, 공개일, 상영 시간, 메타데이터를 저장할 수 없습니다."
			);
		}
	}

	private void validateTitle(String title) {
		if (title == null || title.isBlank() || title.length() > 255) {
			throw new IllegalArgumentException("콘텐츠 제목은 필수이며 255자 이하여야 합니다.");
		}
	}

	private static void validateOptionalThumbnailUrl(String thumbnailUrl) {
		if (thumbnailUrl != null
			&& (thumbnailUrl.isBlank() || thumbnailUrl.length() > MAX_THUMBNAIL_URL_LENGTH)) {
			throw new IllegalArgumentException("썸네일 URL은 500자 이하이거나 null이어야 합니다.");
		}
	}

	public enum AiTaggingStatus {
		PENDING,
		COMPLETED,
		COMPLETED_PARTIAL,
		FAILED
	}
}
