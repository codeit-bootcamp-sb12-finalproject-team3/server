package com.moduplaylist.api.review.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

	@Size(max = 2048)
	private String cursor;

	private UUID idAfter;

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
	@AssertTrue(message = "cursor와 idAfter는 함께 전달해야 합니다.")
	public boolean isCursorValid() {
		boolean cursorEmpty = cursor == null || cursor.isBlank();
		return cursorEmpty == (idAfter == null);
	}
}
