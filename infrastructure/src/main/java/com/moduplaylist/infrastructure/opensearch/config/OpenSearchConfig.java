package com.moduplaylist.infrastructure.opensearch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenSearchProperties.class)
@ConditionalOnProperty(prefix = "mopl.opensearch", name = "enabled", havingValue = "true")
public class OpenSearchConfig {

    @Bean(destroyMethod = "close")
    public RestClient openSearchRestClient(OpenSearchProperties properties) {
        List<String> uris = properties.getUris();
        if (uris == null || uris.isEmpty()) {
            throw new IllegalArgumentException("OpenSearch URI는 한 개 이상 필요합니다.");
        }

        HttpHost[] hosts = uris.stream()
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);
        RestClientBuilder builder = RestClient.builder(hosts);

        if (!properties.getUsername().isBlank()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(
                            properties.getUsername(),
                            properties.getPassword()
                    )
            );
            builder.setHttpClientConfigCallback(
                    httpClientBuilder -> httpClientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider)
            );
        }
        return builder.build();
    }

    @Bean
    public OpenSearchTransport openSearchTransport(
            RestClient openSearchRestClient,
            ObjectMapper objectMapper
    ) {
        return new RestClientTransport(
                openSearchRestClient,
                new JacksonJsonpMapper(objectMapper)
        );
    }

    @Bean
    public OpenSearchClient openSearchClient(OpenSearchTransport openSearchTransport) {
        return new OpenSearchClient(openSearchTransport);
    }
}
