package com.moduplaylist.api.content.service.impl;

import com.moduplaylist.api.content.service.ContentAutocompletePopularityScoreProvider;
import com.moduplaylist.infrastructure.redis.trending.TrendingContentRedisRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrendingContentAutocompletePopularityScoreProvider
        implements ContentAutocompletePopularityScoreProvider {

    private final TrendingContentRedisRepository trendingContentRedisRepository;

    @Override
    public Map<UUID, Double> findScores(Collection<UUID> contentIds) {
        return trendingContentRedisRepository.findScores(contentIds, Instant.now());
    }
}
