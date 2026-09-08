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
