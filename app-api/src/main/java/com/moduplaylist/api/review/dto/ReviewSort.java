package com.moduplaylist.api.review.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum ReviewSort {
	CREATED_AT("createdAt"),
	RATING("rating");

	private final String value;

	ReviewSort(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static ReviewSort fromValue(String value) {
		return Arrays.stream(values())
			.filter(sort -> sort.value.equals(value))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				"지원하지 않는 리뷰 정렬 기준입니다: " + value));
	}
}
