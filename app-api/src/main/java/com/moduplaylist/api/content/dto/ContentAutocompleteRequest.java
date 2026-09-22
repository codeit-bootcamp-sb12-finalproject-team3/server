package com.moduplaylist.api.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ContentAutocompleteRequest {

	@NotBlank
	@Size(min = 2, max = 100)
	private String query;

	public void setQuery(String query) {
		this.query = query == null ? null : query.strip();
	}

}
