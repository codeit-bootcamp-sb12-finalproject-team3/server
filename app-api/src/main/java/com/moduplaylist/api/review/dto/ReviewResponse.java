package com.moduplaylist.api.review.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID userId;
    private UUID contentId;
    private String reviewText;
    private BigDecimal rating;
    private boolean spoiler;
    private Instant createdAt;
    private Instant updatedAt;
}
