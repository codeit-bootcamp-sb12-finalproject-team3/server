package com.moduplaylist.batch.job.contenttagging;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mopl.batch.content-tagging")
public class ContentTaggingProperties {
    private String model = "gpt-4o-mini";
    private int timeoutSeconds = 20;
    private int maxTokens = 1024;
    private int maxItems = 500;
    private Map<String, String> synonyms = new LinkedHashMap<>(Map.of("시간여행", "시간 여행"));
}
