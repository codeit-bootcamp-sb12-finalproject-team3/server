package com.moduplaylist.api.review.dto;

import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** 부분 수정 요청. null인 필드는 기존 값을 유지한다. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewUpdateRequest {
    @Pattern(regexp = "(?s).*\\S.*", message = "리뷰 내용은 공백일 수 없습니다.")
    private String reviewText;
    @DecimalMin("0.0") @DecimalMax("5.0")
    private BigDecimal rating;
    private Boolean spoiler;

    @AssertTrue(message = "평점은 0.5점 단위여야 합니다.")
    @JsonIgnore
    public boolean isRatingStepValid() {
        return rating == null || rating.remainder(new BigDecimal("0.5")).signum() == 0;
    }
}
