package com.moduplaylist.api.content.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CastResponse {

	private String name;
	private String roleName;
	private String profileImageUrl;
}
