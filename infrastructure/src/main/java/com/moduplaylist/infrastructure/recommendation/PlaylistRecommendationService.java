package com.moduplaylist.infrastructure.recommendation;

import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.entity.PlaylistSubscription;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.core.playlist.repository.PlaylistSubscriptionRepository;
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
    private final PlaylistSubscriptionRepository subscriptionRepository;
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

        Set<UUID> existingOtherOwnerIds = new HashSet<>(
                playlistRepository.findAllById(candidateIds).stream()
                        .filter(playlist -> !playlist.getOwner().getId().equals(userId))
                        .map(Playlist::getId)
                        .toList()
        );
        Set<UUID> subscribedIds = new HashSet<>(
                subscriptionRepository.findAllByUser_IdAndPlaylist_IdIn(userId, candidateIds)
                        .stream()
                        .map(PlaylistSubscription::getPlaylist)
                        .map(Playlist::getId)
                        .toList()
        );

        return candidateIds.stream()
                .filter(existingOtherOwnerIds::contains)
                .filter(playlistId -> !subscribedIds.contains(playlistId))
                .limit(properties.getSearchLimit())
                .toList();
    }
}
