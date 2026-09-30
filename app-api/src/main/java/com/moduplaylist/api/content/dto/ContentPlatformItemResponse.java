package com.moduplaylist.api.content.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentPlatformItemResponse {

	private UUID platformId;
	private String name;
	private String logoUrl;
	private String url;
}
