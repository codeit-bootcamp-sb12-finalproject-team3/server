package com.moduplaylist.api.content.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlatformResponse {

	private UUID id;
	private String name;
	private String logoUrl;
}
