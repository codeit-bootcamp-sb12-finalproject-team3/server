package com.moduplaylist.api.content.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mopl.search.semantic")
public class ContentSemanticSearchProperties {

	private boolean enabled = true;
	private int candidateLimit = 100;
	private int minimumQueryLength = 3;
	private int rrfK = 60;
	private double keywordWeight = 1.0;
	private double semanticWeight = 0.7;
}
