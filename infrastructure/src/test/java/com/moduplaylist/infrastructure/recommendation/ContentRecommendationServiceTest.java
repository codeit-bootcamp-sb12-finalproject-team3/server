package com.moduplaylist.infrastructure.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moduplaylist.core.content.repository.ContentLikeRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.recommendation.repository.UserPreferenceContentRepository;
import com.moduplaylist.core.review.repository.ReviewRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorSearchRepository;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserContentPreferenceVectorDocument;
import com.moduplaylist.infrastructure.opensearch.recommendation.UserContentPreferenceVectorRepository;
import com.moduplaylist.infrastructure.redis.recommendation.ContentRecommendationRedisRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentRecommendationServiceTest {

    @Mock
    private UserContentPreferenceVectorRepository userVectorRepository;

    @Mock
    private ContentVectorSearchRepository contentVectorSearchRepository;

    @Mock
    private UserPreferenceContentRepository userPreferenceContentRepository;

    @Mock
    private ContentLikeRepository contentLikeRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentRecommendationRedisRepository recommendationRedisRepository;

    @Mock
    private RecommendationProperties properties;

    @InjectMocks
    private ContentRecommendationService contentRecommendationService;

    @Test
    void generateAndCache_excludesPreferredLikedAndRatedContents() {
        UUID userId = UUID.randomUUID();
        UUID preferredContentId = UUID.randomUUID();
        UUID likedContentId = UUID.randomUUID();
        UUID ratedContentId = UUID.randomUUID();
        UUID duplicatedContentId = UUID.randomUUID();
        UserContentPreferenceVectorDocument preferenceVector =
                UserContentPreferenceVectorDocument.builder()
                        .userId(userId)
                        .embedding(new float[] {0.1f})
                        .build();

        when(userVectorRepository.findById(userId)).thenReturn(Optional.of(preferenceVector));
        when(userPreferenceContentRepository.findContentIdsByUserId(userId))
                .thenReturn(List.of(preferredContentId, duplicatedContentId));
        when(contentLikeRepository.findContentIdsByUserId(userId))
                .thenReturn(List.of(likedContentId, duplicatedContentId));
        when(reviewRepository.findContentIdsByUserId(userId))
                .thenReturn(List.of(ratedContentId, duplicatedContentId));
        when(properties.getSearchLimit()).thenReturn(100);
        when(contentVectorSearchRepository.findNearest(any(), any(), eq(100)))
                .thenReturn(List.of());

        contentRecommendationService.generateAndCache(userId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> excludedContentIdsCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(contentVectorSearchRepository)
                .findNearest(any(), excludedContentIdsCaptor.capture(), eq(100));
        assertThat(excludedContentIdsCaptor.getValue())
                .containsExactly(
                        preferredContentId,
                        duplicatedContentId,
                        likedContentId,
                        ratedContentId);
        verify(recommendationRedisRepository).replace(userId, List.of());
    }
}
