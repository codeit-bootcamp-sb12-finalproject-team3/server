package com.moduplaylist.infrastructure.opensearch.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.Request;
import org.opensearch.client.RestClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.opensearch", name = "enabled", havingValue = "true")
public class OpenSearchIndexInitializer {

    private final OpenSearchClient openSearchClient;
    private final RestClient restClient;
    private final OpenSearchProperties properties;

    @PostConstruct
    public void initialize() throws IOException {
        createIndexIfAbsent(
                properties.getUserPreferenceIndex(),
                "opensearch/user-preference-index.json"
        );
        createIndexIfAbsent(
                properties.getContentIndex(),
                "opensearch/content-index.json"
        );
        ensureContentHiddenMapping();
    }

    private void createIndexIfAbsent(String indexName, String mappingPath) throws IOException {
        boolean exists = openSearchClient.indices()
                .exists(request -> request.index(indexName))
                .value();
        if (exists) {
            return;
        }

        ClassPathResource mapping = new ClassPathResource(mappingPath);
        String mappingJson = mapping.getContentAsString(StandardCharsets.UTF_8);
        Request request = new Request("PUT", "/" + indexName);
        request.setJsonEntity(mappingJson);
        restClient.performRequest(request);
    }

    private void ensureContentHiddenMapping() throws IOException {
        Request request = new Request(
                "PUT",
                "/" + properties.getContentIndex() + "/_mapping"
        );
        request.setJsonEntity("""
                {
                  "properties": {
                    "hidden": { "type": "boolean" },
                    "sportType": { "type": "text" },
                    "leagueName": { "type": "text" },
                    "homeTeamName": { "type": "text" },
                    "awayTeamName": { "type": "text" },
                    "venue": { "type": "text" }
                  }
                }
                """);
        restClient.performRequest(request);
    }
}
