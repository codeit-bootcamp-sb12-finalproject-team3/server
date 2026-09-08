package com.moduplaylist.core.content;

import com.moduplaylist.core.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contents", indexes = {
        @Index(name = "idx_contents_created", columnList = "created_at DESC, id DESC"),
        @Index(name = "idx_contents_rating", columnList = "average_rating DESC, id DESC"),
        @Index(name = "idx_contents_type_created", columnList = "type, created_at DESC, id DESC"),
        @Index(name = "idx_contents_type_rating", columnList = "type, average_rating DESC, id DESC"),
        @Index(name = "idx_contents_sport_created", columnList = "sport_type, created_at DESC, id DESC")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_contents_external",
                columnNames = {"external_source", "type", "external_id"}),
        @UniqueConstraint(name = "uq_contents_parent_season",
                columnNames = {"parent_content_id", "season_number"})
})
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseEntity {
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

    @Builder.Default
    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = new BigDecimal("0.00");

    @Builder.Default
    @Column(name = "like_count", nullable = false, columnDefinition = "INT UNSIGNED")
    private long likeCount = 0;

    @Builder.Default
    @Column(name = "review_count", nullable = false, columnDefinition = "INT UNSIGNED")
    private long reviewCount = 0;

    /** 연관 데이터에 따른 타입 변경 가능 여부는 Service에서 먼저 확인한다. */
    public void updateDetails(String title, String description, String thumbnailUrl, ContentType type) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (thumbnailUrl != null) this.thumbnailUrl = thumbnailUrl;
        if (type != null && this.type != type) {
            this.type = type;
            if (type != ContentType.TV_SEASON) {
                this.parentContent = null;
                this.seasonNumber = null;
                this.episodeCount = null;
            }
            if (type != ContentType.TV_SERIES) this.seasonCount = null;
            if (type != ContentType.SPORT) this.sportType = null;
        }
    }

    public void updateReviewStatistics(BigDecimal averageRating, long reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    // DTO 검증과 별개로 배치 등 다른 저장 경로에서도 도메인 규칙을 지킨다.
    @PrePersist
    @PreUpdate
    void validate() {
        if (title == null || title.isBlank() || title.length() > 255 || type == null) {
            throw new IllegalArgumentException("콘텐츠 제목과 타입은 필수입니다. 제목은 255자 이하여야 합니다.");
        }
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
        if ((externalSource == null) != (externalId == null)) {
            throw new IllegalArgumentException("외부 출처와 외부 ID는 함께 지정해야 합니다.");
        }
        if ((runtime != null && runtime <= 0)
                || (seasonNumber != null && seasonNumber < 0)
                || (seasonCount != null && seasonCount < 0)
                || (episodeCount != null && episodeCount < 0)) {
            throw new IllegalArgumentException("러닝타임은 양수, 시즌 번호와 개수는 0 이상이어야 합니다.");
        }
        if (averageRating == null || averageRating.signum() < 0
                || averageRating.compareTo(new BigDecimal("5.00")) > 0
                || likeCount < 0 || likeCount > 4_294_967_295L
                || reviewCount < 0 || reviewCount > 4_294_967_295L) {
            throw new IllegalArgumentException("평점은 0~5, 집계 수는 unsigned INT 범위여야 합니다.");
        }
    }
}
