package com.moduplaylist.api.content.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentLikeResponseDto {

	private boolean liked;
	private long likeCount;
}
