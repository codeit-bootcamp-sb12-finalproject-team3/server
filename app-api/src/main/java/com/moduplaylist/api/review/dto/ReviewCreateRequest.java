package com.moduplaylist.api.review.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewCreateRequest {
	private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

	@NotNull
	private UUID contentId;

	@NotBlank
	@Size(max = 800)
	private String text;

	@NotNull
	@DecimalMin("0.5")
	@DecimalMax("5.0")
	private BigDecimal rating;

	public void setText(String text) {
		this.text = text == null ? null : text.strip();
	}

	@JsonIgnore
	@AssertTrue(message = "rating은 0.5 단위여야 합니다.")
	public boolean isRatingStepValid() {
		return rating == null
			|| rating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) == 0;
	}
}
