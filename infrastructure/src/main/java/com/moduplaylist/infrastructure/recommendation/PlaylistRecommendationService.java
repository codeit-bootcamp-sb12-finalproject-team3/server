package com.moduplaylist.infrastructure.recommendation;

import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.infrastructure.opensearch.playlist.PlaylistSimilarityCandidate;
import com.moduplaylist.infrastructure.redis.recommendation.PlaylistRecommendationRedisRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.opensearch", name = "enabled", havingValue = "true")
public class PlaylistRecommendationService {

    private final PlaylistRecommendationCandidateService candidateService;
    private final PlaylistRepository playlistRepository;
    private final PlaylistRecommendationRedisRepository recommendationRedisRepository;
    private final RecommendationProperties properties;

    public List<UUID> generateAndCache(UUID userId) {
        List<PlaylistSimilarityCandidate> candidates = candidateService.findCandidates(
                userId, properties.getSearchLimit()
        );
        List<UUID> recommendationIds = keepCurrentlyRecommendablePlaylists(userId, candidates);
        recommendationRedisRepository.replace(userId, recommendationIds);
        return recommendationIds;
    }

    private List<UUID> keepCurrentlyRecommendablePlaylists(
            UUID userId,
            List<PlaylistSimilarityCandidate> candidates
    ) {
        List<UUID> candidateIds = candidates.stream()
                .map(PlaylistSimilarityCandidate::playlistId)
                .distinct()
                .toList();
        if (candidateIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> currentlyRecommendableIds = new HashSet<>(
                playlistRepository.findRecommendableIds(userId, candidateIds)
        );

        return candidateIds.stream()
                .filter(currentlyRecommendableIds::contains)
                .limit(properties.getSearchLimit())
                .toList();
    }
}
