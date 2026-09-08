package com.moduplaylist.core.content;

import com.moduplaylist.core.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "episodes", uniqueConstraints = {
        @UniqueConstraint(name = "uq_episodes_external_id", columnNames = "external_id"),
        @UniqueConstraint(name = "uq_episodes_season_number",
                columnNames = {"season_id", "episode_number"})
})
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Episode extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
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

    @Column(name = "external_id", nullable = false)
    private Integer externalId;

    @PrePersist
    @PreUpdate
    void validate() {
        if (season == null || season.getType() != ContentType.TV_SEASON) {
            throw new IllegalArgumentException("회차는 TV 시즌에 속해야 합니다.");
        }
        if (episodeNumber == null || episodeNumber < 0) {
            throw new IllegalArgumentException("회차 번호는 0 이상이어야 합니다.");
        }
        if (title == null || title.isBlank() || title.length() > 255 || externalId == null) {
            throw new IllegalArgumentException("회차 제목과 외부 ID는 필수입니다. 제목은 255자 이하여야 합니다.");
        }
        if (runtime != null && runtime <= 0) {
            throw new IllegalArgumentException("회차 러닝타임은 양수여야 합니다.");
        }
    }
}
