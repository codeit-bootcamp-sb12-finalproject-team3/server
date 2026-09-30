package com.moduplaylist.api.content.dto;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ContentTypeFilterConverter implements Converter<String, ContentTypeFilter> {

	@Override
	public ContentTypeFilter convert(@NonNull String source) {
		return ContentTypeFilter.fromValue(source);
	}
}
