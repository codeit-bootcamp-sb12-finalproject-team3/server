package com.moduplaylist.api.content.dto;

import com.moduplaylist.core.content.entity.TagSource;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagResponse {

	private UUID id;
	private String name;
	private TagSource source;
}
