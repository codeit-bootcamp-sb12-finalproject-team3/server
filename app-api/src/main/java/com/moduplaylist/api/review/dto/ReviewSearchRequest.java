package com.moduplaylist.api.review.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReviewSearchRequest {

	private UUID contentIdEqual;

	private UUID userIdEqual;

	private Instant cursorCreatedAt;

	private UUID cursorId;

	@Min(1)
	@Max(100)
	@NotNull
	private Integer limit = 20;

	@JsonIgnore
	@AssertTrue(message = "contentIdEqual과 userIdEqual 중 하나만 전달해야 합니다.")
	public boolean isFilterValid() {
		return (contentIdEqual == null) != (userIdEqual == null);
	}

	@JsonIgnore
	@AssertTrue(message = "cursorCreatedAt과 cursorId는 함께 전달해야 합니다.")
	public boolean isCursorValid() {
		return (cursorCreatedAt == null) == (cursorId == null);
	}
}
