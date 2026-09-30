package com.moduplaylist.api.content.service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ContentAutocompletePopularityScoreProvider {

	Map<UUID, Double> findScores(Collection<UUID> contentIds);
}
