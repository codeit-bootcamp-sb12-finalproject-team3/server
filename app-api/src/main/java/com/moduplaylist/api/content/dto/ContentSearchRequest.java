package com.moduplaylist.api.content.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ContentSearchRequest {

	@Size(max = 100)
	private String keywordLike;

	private ContentTypeFilter typeEqual;

	@Size(max = 50)
	private String sportTypeEqual;

	private UUID likedByUserIdEqual;

	private ContentSort sortBy;

	private Instant cursorCreatedAt;

	private Instant cursorLikedAt;

	@DecimalMin("0.0")
	@DecimalMax("5.0")
	private BigDecimal cursorRating;

	private UUID cursorId;

	@Min(1)
	@Max(100)
	@NotNull
	private Integer limit = 20;

	public void setKeywordLike(String value) {
		this.keywordLike = normalize(value);
	}

	public void setSportTypeEqual(String value) {
		this.sportTypeEqual = normalize(value);
	}

	private static String normalize(String value) {
		return value == null ? null : value.strip();
	}
}
