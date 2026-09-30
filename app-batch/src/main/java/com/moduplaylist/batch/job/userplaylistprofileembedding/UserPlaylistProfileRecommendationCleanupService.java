package com.moduplaylist.batch.job.userplaylistprofileembedding;

import com.moduplaylist.infrastructure.opensearch.recommendation.UserPlaylistPreferenceVectorRepository;
import com.moduplaylist.infrastructure.redis.recommendation.PlaylistRecommendationRedisRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPlaylistProfileRecommendationCleanupService {

    private final UserPlaylistPreferenceVectorRepository userPreferenceVectorRepository;
    private final PlaylistRecommendationRedisRepository recommendationRedisRepository;

    public void removeStaleRecommendation(UUID userId) {
        userPreferenceVectorRepository.deleteById(userId);
        recommendationRedisRepository.delete(userId);
    }
}
