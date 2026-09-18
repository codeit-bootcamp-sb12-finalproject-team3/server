package com.moduplaylist.infrastructure.recommendation;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.repository.ContentLikeRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.recommendation.repository.UserPreferenceContentRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentSimilarityCandidate;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorSearchRepository;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserContentPreferenceVectorDocument;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserContentPreferenceVectorRepository;
import com.moduplaylist.infrastructure.redis.recommendation.ContentRecommendationRedisRepository;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentRecommendationService {

    private final UserContentPreferenceVectorRepository userVectorRepository;
    private final ContentVectorSearchRepository contentVectorSearchRepository;
    private final UserPreferenceContentRepository userPreferenceContentRepository;
    private final ContentLikeRepository contentLikeRepository;
    private final ContentRepository contentRepository;
    private final ContentRecommendationRedisRepository recommendationRedisRepository;
    private final RecommendationProperties properties;

    public List<UUID> generateAndCache(UUID userId) {
        UserContentPreferenceVectorDocument userVector = userVectorRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "사용자 선호 벡터가 존재하지 않습니다. userId=" + userId
                ));

        Set<UUID> excludedContentIds = findExcludedContentIds(userId);
        List<ContentSimilarityCandidate> candidates = contentVectorSearchRepository.findNearest(
                userVector.getEmbedding(),
                excludedContentIds,
                properties.getSearchLimit()
        );
        List<UUID> recommendationIds = keepExistingRecommendableContents(candidates);
        recommendationRedisRepository.replace(userId, recommendationIds);
        return recommendationIds;
    }

    private Set<UUID> findExcludedContentIds(UUID userId) {
        Set<UUID> excludedContentIds = new LinkedHashSet<>();
        excludedContentIds.addAll(userPreferenceContentRepository.findContentIdsByUserId(userId));
        excludedContentIds.addAll(contentLikeRepository.findContentIdsByUserId(userId));
        return excludedContentIds;
    }

    private List<UUID> keepExistingRecommendableContents(
            List<ContentSimilarityCandidate> candidates
    ) {
        List<UUID> candidateIds = candidates.stream()
                .map(ContentSimilarityCandidate::contentId)
                .toList();
        Set<UUID> existingRecommendableIds = new HashSet<>(
                contentRepository.findAllById(candidateIds).stream()
                        .filter(content -> content.getType().isPersonalizable())
                        .map(Content::getId)
                        .toList()
        );

        return candidateIds.stream()
                .filter(existingRecommendableIds::contains)
                .distinct()
                .limit(properties.getSearchLimit())
                .toList();
    }
}
