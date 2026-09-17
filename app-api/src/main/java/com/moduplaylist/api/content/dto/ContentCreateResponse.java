package com.moduplaylist.api.content.dto;

import com.moduplaylist.core.content.entity.ContentType;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentCreateResponse {

	private UUID id;
	private String title;
	private ContentType type;
	private Instant createdAt;
}
