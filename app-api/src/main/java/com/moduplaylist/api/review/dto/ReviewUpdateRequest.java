package com.moduplaylist.api.review.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewUpdateRequest {
	private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

	@Size(max = 800)
	private String text;

	@JsonSetter(nulls = Nulls.FAIL)
	@DecimalMin("0.5")
	@DecimalMax("5.0")
	private BigDecimal rating;

	@JsonSetter(value = "text", nulls = Nulls.FAIL)
	public void setText(String text) {
		this.text = text.strip();
	}

	@JsonIgnore
	@AssertTrue(message = "text는 공백일 수 없습니다.")
	public boolean isTextValid() {
		return text == null || !text.isBlank();
	}

	@JsonIgnore
	@AssertTrue(message = "rating은 0.5 단위여야 합니다.")
	public boolean isRatingStepValid() {
		return rating == null
			|| rating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) == 0;
	}
}
