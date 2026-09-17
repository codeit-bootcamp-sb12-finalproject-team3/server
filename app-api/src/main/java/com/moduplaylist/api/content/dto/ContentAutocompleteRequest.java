package com.moduplaylist.api.content.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ContentAutocompleteRequest {

	@NotBlank
	@Size(max = 100)
	private String query;

	@NotNull
	@Min(1)
	@Max(20)
	private Integer limit = 10;

	public void setQuery(String query) {
		this.query = query == null ? null : query.strip();
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}
}
