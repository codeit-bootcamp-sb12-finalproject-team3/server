package com.moduplaylist.api.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ContentPlatformCreateRequest {

	@NotNull
	private UUID platformId;

	@NotBlank
	@Size(max = 1000)
	@Pattern(regexp = "https?://.+")
	private String url;

	public void setPlatformId(UUID platformId) {
		this.platformId = platformId;
	}

	public void setUrl(String url) {
		this.url = url == null ? null : url.strip();
	}
}
