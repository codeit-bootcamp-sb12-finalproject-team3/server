package com.moduplaylist.infrastructure.opensearch.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mopl.opensearch")
public class OpenSearchProperties {

    private boolean enabled;
    private List<String> uris = List.of("http://localhost:9200");
    private String username = "";
    private String password = "";
    private String userPreferenceIndex = "user-preference-index";
    private String contentIndex = "content-index";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserPreferenceIndex() {
        return userPreferenceIndex;
    }

    public void setUserPreferenceIndex(String userPreferenceIndex) {
        this.userPreferenceIndex = userPreferenceIndex;
    }

    public String getContentIndex() {
        return contentIndex;
    }

    public void setContentIndex(String contentIndex) {
        this.contentIndex = contentIndex;
    }
}
