package com.moduplaylist.batch.job.userprofileembedding;

import com.moduplaylist.infrastructure.opensearch.recommendation.UserPreferenceVectorRepository;
import com.moduplaylist.infrastructure.redis.recommendation.ContentRecommendationRedisRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileRecommendationCleanupService {

    private final UserPreferenceVectorRepository userPreferenceVectorRepository;
    private final ContentRecommendationRedisRepository recommendationRedisRepository;

    public void removeStaleRecommendation(UUID userId) {
        userPreferenceVectorRepository.deleteById(userId);
        recommendationRedisRepository.delete(userId);
    }
}
