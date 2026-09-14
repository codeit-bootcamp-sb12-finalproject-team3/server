package com.moduplaylist.api.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ContentCastUpdateRequest {

	@NotBlank
	@Size(max = 100)
	private String name;

	@Size(max = 255)
	private String roleName;

	@Size(max = 500)
	private String profileImageUrl;

	public void setName(String name) {
		this.name = normalizeRequired(name);
	}

	public void setRoleName(String roleName) {
		this.roleName = normalizeOptional(roleName);
	}

	public void setProfileImageUrl(String profileImageUrl) {
		this.profileImageUrl = normalizeOptional(profileImageUrl);
	}

	private static String normalizeRequired(String value) {
		return value == null ? null : value.strip();
	}

	private static String normalizeOptional(String value) {
		if (value == null) {
			return null;
		}
		String stripped = value.strip();
		return stripped.isEmpty() ? null : stripped;
	}
}
