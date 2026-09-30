package com.moduplaylist.api.content.dto;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ContentSortConverter implements Converter<String, ContentSort> {

	@Override
	public ContentSort convert(@NonNull String source) {
		return ContentSort.fromValue(source);
	}
}
