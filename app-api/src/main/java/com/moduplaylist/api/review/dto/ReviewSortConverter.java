package com.moduplaylist.api.review.dto;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ReviewSortConverter implements Converter<String, ReviewSort> {

	@Override
	public ReviewSort convert(String source) {
		return ReviewSort.fromValue(source);
	}
}
