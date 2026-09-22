package com.moduplaylist.infrastructure.sportsdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mopl.sports-db")
public class SportsDbProperties {
    private String apiBaseUrl = "https://www.thesportsdb.com/api/v1/json";
    private String apiKey = "123";
    private int timeoutSeconds = 10;
    private long requestIntervalMillis = 2000;

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public long getRequestIntervalMillis() { return requestIntervalMillis; }
    public void setRequestIntervalMillis(long requestIntervalMillis) {
        this.requestIntervalMillis = requestIntervalMillis;
    }
}
