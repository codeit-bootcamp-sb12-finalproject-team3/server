package com.moduplaylist.api.review.dto;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ReviewSortConverter implements Converter<String, ReviewSort> {

	@Override
	public ReviewSort convert(@NonNull String source) {
		return ReviewSort.fromValue(source);
	}
}
