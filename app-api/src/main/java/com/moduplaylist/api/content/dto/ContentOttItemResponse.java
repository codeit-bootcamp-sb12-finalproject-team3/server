package com.moduplaylist.api.content.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentOttItemResponse {

	private UUID ottId;
	private String name;
	private String logoUrl;
	private String watchUrl;
}
