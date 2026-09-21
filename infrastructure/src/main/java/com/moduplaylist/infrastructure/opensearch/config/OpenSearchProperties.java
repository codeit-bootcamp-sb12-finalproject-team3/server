package com.moduplaylist.infrastructure.opensearch.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mopl.opensearch")
public class OpenSearchProperties {

    private boolean enabled;
    private List<String> uris = List.of("http://localhost:9200");
    private String username = "";
    private String password = "";
    private String userContentPreferenceIndex = "user-content-preference-index";
    private String userPlaylistPreferenceIndex = "user-playlist-preference-index";
    private String contentIndex = "content-index";
    private String contentAutocompleteIndex = "content-autocomplete-index";
    private String playlistIndex = "playlist-index";

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

    public String getUserContentPreferenceIndex() {
        return userContentPreferenceIndex;
    }

    public void setUserContentPreferenceIndex(String userContentPreferenceIndex) {
        this.userContentPreferenceIndex = userContentPreferenceIndex;
    }

    public String getUserPlaylistPreferenceIndex() {
        return userPlaylistPreferenceIndex;
    }

    public void setUserPlaylistPreferenceIndex(String userPlaylistPreferenceIndex) {
        this.userPlaylistPreferenceIndex = userPlaylistPreferenceIndex;
    }

    public String getContentIndex() {
        return contentIndex;
    }

    public void setContentIndex(String contentIndex) {
        this.contentIndex = contentIndex;
    }

    public String getContentAutocompleteIndex() {
        return contentAutocompleteIndex;
    }

    public void setContentAutocompleteIndex(String contentAutocompleteIndex) {
        this.contentAutocompleteIndex = contentAutocompleteIndex;
    }

    public String getPlaylistIndex() {
        return playlistIndex;
    }

    public void setPlaylistIndex(String playlistIndex) {
        this.playlistIndex = playlistIndex;
    }
}
