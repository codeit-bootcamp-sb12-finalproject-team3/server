package com.moduplaylist.api.content.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentSummaryResponse {
    private UUID id;
    private String title;
    private String type;
    private String thumbnailUrl;
    private String sportType;
    private Integer seasonNumber;
    private LocalDate releaseDate;
    private BigDecimal averageRating;
    private long likeCount;
    private long reviewCount;
    private Instant createdAt;
}
