package com.moduplaylist.batch.job.contentimport;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mopl.sports-import")
public class SportsImportProperties {
    private List<League> leagues = new ArrayList<>();

    public List<League> getLeagues() {
        return leagues;
    }

    public void setLeagues(List<League> leagues) {
        this.leagues = leagues;
    }

    public static class League {
        private String sportCode;
        private String externalLeagueId;
        private String name;
        private boolean enabled = true;

        public String getSportCode() { return sportCode; }
        public void setSportCode(String sportCode) { this.sportCode = sportCode; }
        public String getExternalLeagueId() { return externalLeagueId; }
        public void setExternalLeagueId(String externalLeagueId) { this.externalLeagueId = externalLeagueId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
