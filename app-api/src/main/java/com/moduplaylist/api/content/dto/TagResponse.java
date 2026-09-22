package com.moduplaylist.api.content.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagResponse {

	private UUID id;
	private String name;
}
