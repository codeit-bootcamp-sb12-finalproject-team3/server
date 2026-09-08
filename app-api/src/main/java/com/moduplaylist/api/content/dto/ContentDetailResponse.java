package com.moduplaylist.api.content.dto;

import lombok.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * 콘텐츠 자체의 상세 정보. 다른 도메인의 연관 응답은 해당 조회 기능 구현 시 확장한다.
 */
public class ContentDetailResponse {
    private UUID id;
    private UUID parentContentId;
    private String title;
    private String type;
    private String description;
    private String thumbnailUrl;
    private String sportType;
    private Integer seasonNumber;
    private Integer seasonCount;
    private Integer episodeCount;
    private LocalDate releaseDate;
    private Integer runtime;
    private Map<String, Object> metadata;
    private BigDecimal averageRating;
    private long likeCount;
    private long reviewCount;
    private Instant createdAt;
    private Instant updatedAt;
    @Builder.Default
    private List<EpisodeResponse> episodes = List.of();
    @Builder.Default
    private List<ContentSummaryResponse> seasons = List.of();
    @Builder.Default
    private List<com.moduplaylist.core.content.repository.ContentRelationRepository.Genre> genres = List.of();
    @Builder.Default
    private List<com.moduplaylist.core.content.repository.ContentRelationRepository.Tag> tags = List.of();
    @Builder.Default
    private List<com.moduplaylist.core.content.repository.ContentRelationRepository.Cast> casts = List.of();
    @Builder.Default
    private List<com.moduplaylist.core.content.repository.ContentRelationRepository.Ott> otts = List.of();
    @Builder.Default
    private List<com.moduplaylist.core.content.repository.ContentRelationRepository.Playlist> playlists = List.of();
    @Builder.Default
    private List<com.moduplaylist.core.content.repository.ContentRelationRepository.WatchParty> watchParties = List.of();
}
