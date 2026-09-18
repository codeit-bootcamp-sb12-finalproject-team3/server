package com.moduplaylist.api.review.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewUpdateRequest {
	private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

	@JsonSetter(nulls = Nulls.FAIL)
	private String content;

	@JsonSetter(nulls = Nulls.FAIL)
	@DecimalMin("0.5")
	@DecimalMax("5.0")
	private BigDecimal rating;

	@JsonSetter(nulls = Nulls.FAIL)
	@JsonProperty("isSpoiler")
	private Boolean isSpoiler;

	public boolean hasAnyField() {
		return content != null || rating != null || isSpoiler != null;
	}

	@JsonIgnore
	@AssertTrue(message = "수정할 필드를 하나 이상 전달해야 합니다.")
	public boolean isAnyFieldPresent() {
		return hasAnyField();
	}

	@JsonIgnore
	@AssertTrue(message = "content는 공백일 수 없습니다.")
	public boolean isContentValid() {
		return content == null || !content.isBlank();
	}

	@JsonIgnore
	@AssertTrue(message = "rating은 0.5 단위여야 합니다.")
	public boolean isRatingStepValid() {
		return rating == null
			|| rating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) == 0;
	}
}
