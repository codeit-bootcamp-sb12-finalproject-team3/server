package com.moduplaylist.api.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum ContentSort {
	LATEST("latest"),
	RATING("rating");

	private final String value;

	ContentSort(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static ContentSort fromValue(String value) {
		return Arrays.stream(values())
			.filter(sort -> sort.value.equals(value))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				"지원하지 않는 콘텐츠 정렬 기준입니다: " + value));
	}
}
