package com.moduplaylist.api.review.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.moduplaylist.api.global.dto.SortDirection;
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

	private UUID contentId;

	@Size(max = 2048)
	private String cursor;

	private UUID idAfter;

	@Min(1)
	@Max(100)
	@NotNull
	private Integer limit;

	@NotNull
	private ReviewSort sortBy;

	@NotNull
	private SortDirection sortDirection;

	@JsonIgnore
	@AssertTrue(message = "cursor와 idAfter는 함께 전달해야 합니다.")
	public boolean isCursorValid() {
		boolean cursorEmpty = cursor == null || cursor.isBlank();
		return cursorEmpty == (idAfter == null);
	}
}
