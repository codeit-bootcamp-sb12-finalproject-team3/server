package com.moduplaylist.infrastructure.recommendation;

import com.moduplaylist.core.playlist.repository.PlaylistSubscriptionRepository;
import com.moduplaylist.infrastructure.opensearch.playlist.PlaylistSimilarityCandidate;
import com.moduplaylist.infrastructure.opensearch.playlist.PlaylistVectorSearchRepository;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserPlaylistPreferenceVectorDocument;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserPlaylistPreferenceVectorRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.opensearch", name = "enabled", havingValue = "true")
public class PlaylistRecommendationCandidateService {

    private final UserPlaylistPreferenceVectorRepository userVectorRepository;
    private final PlaylistSubscriptionRepository subscriptionRepository;
    private final PlaylistVectorSearchRepository playlistVectorSearchRepository;

    public List<PlaylistSimilarityCandidate> findCandidates(UUID userId, int limit) {
        UserPlaylistPreferenceVectorDocument userVector = userVectorRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "사용자 플레이리스트 선호 벡터가 존재하지 않습니다. userId=" + userId
                ));

        List<UUID> subscribedPlaylistIds = subscriptionRepository.findPlaylistIdsByUserId(userId);
        return playlistVectorSearchRepository.findNearest(
                userVector.getEmbedding(),
                userId,
                subscribedPlaylistIds,
                limit
        );
    }
}
