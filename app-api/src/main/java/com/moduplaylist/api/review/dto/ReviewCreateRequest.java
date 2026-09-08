package com.moduplaylist.api.review.dto;

import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCreateRequest {
    @NotNull
    private UUID contentId;
    @NotBlank
    private String reviewText;
    @NotNull @DecimalMin("0.0") @DecimalMax("5.0")
    private BigDecimal rating;
    private boolean spoiler;

    @AssertTrue(message = "평점은 0.5점 단위여야 합니다.")
    @JsonIgnore
    public boolean isRatingStepValid() {
        return rating == null || rating.remainder(new BigDecimal("0.5")).signum() == 0;
    }
}
