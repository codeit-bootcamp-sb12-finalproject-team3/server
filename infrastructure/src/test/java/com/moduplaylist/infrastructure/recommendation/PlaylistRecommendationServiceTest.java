package com.moduplaylist.infrastructure.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.infrastructure.opensearch.playlist.PlaylistSimilarityCandidate;
import com.moduplaylist.infrastructure.redis.recommendation.PlaylistRecommendationRedisRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaylistRecommendationServiceTest {

    @Mock
    private PlaylistRecommendationCandidateService candidateService;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistRecommendationRedisRepository recommendationRedisRepository;

    @Mock
    private RecommendationProperties properties;

    @InjectMocks
    private PlaylistRecommendationService playlistRecommendationService;

    @Test
    void generateAndCache_keepsRepositoryFilteredIdsInSimilarityOrder() {
        UUID userId = UUID.randomUUID();
        UUID ownedPlaylistId = UUID.randomUUID();
        UUID firstRecommendableId = UUID.randomUUID();
        UUID subscribedPlaylistId = UUID.randomUUID();
        UUID missingPlaylistId = UUID.randomUUID();
        UUID secondRecommendableId = UUID.randomUUID();
        List<PlaylistSimilarityCandidate> candidates = List.of(
                new PlaylistSimilarityCandidate(ownedPlaylistId, 0.99),
                new PlaylistSimilarityCandidate(firstRecommendableId, 0.95),
                new PlaylistSimilarityCandidate(subscribedPlaylistId, 0.90),
                new PlaylistSimilarityCandidate(missingPlaylistId, 0.85),
                new PlaylistSimilarityCandidate(secondRecommendableId, 0.80),
                new PlaylistSimilarityCandidate(firstRecommendableId, 0.75)
        );
        List<UUID> distinctCandidateIds = List.of(
                ownedPlaylistId,
                firstRecommendableId,
                subscribedPlaylistId,
                missingPlaylistId,
                secondRecommendableId
        );

        when(properties.getSearchLimit()).thenReturn(5);
        when(candidateService.findCandidates(userId, 5)).thenReturn(candidates);
        when(playlistRepository.findRecommendableIds(userId, distinctCandidateIds))
                .thenReturn(List.of(secondRecommendableId, firstRecommendableId));

        List<UUID> result = playlistRecommendationService.generateAndCache(userId);

        assertThat(result).containsExactly(firstRecommendableId, secondRecommendableId);
        verify(playlistRepository).findRecommendableIds(userId, distinctCandidateIds);
        verify(recommendationRedisRepository).replace(userId, result);
    }
}
