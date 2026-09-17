package com.moduplaylist.api.recommendation.service.impl;

import com.moduplaylist.api.recommendation.service.ContentPreferenceUpdateService;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentGenre;
import com.moduplaylist.core.content.entity.ContentTag;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.core.recommendation.policy.ContentRecommendationScorePolicy;
import com.moduplaylist.core.recommendation.repository.ContentPreferenceScoreRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ContentPreferenceUpdateServiceImpl implements ContentPreferenceUpdateService {

    private final ContentRepository contentRepository;
    private final ContentGenreRepository contentGenreRepository;
    private final ContentTagRepository contentTagRepository;
    private final UserRepository userRepository;
    private final ContentPreferenceScoreRepository contentPreferenceScoreRepository;

    @Override
    public void applyActivity(
            UUID userId,
            UUID contentId,
            ContentActivityType activityType
    ) {
        if (activityType == ContentActivityType.INITIAL_PREFERENCE) {
            throw new IllegalArgumentException(
                    "INITIAL_PREFERENCE must be handled by the initial preference flow"
            );
        }

        double delta = ContentRecommendationScorePolicy.calculate(activityType);
        applyDelta(userId, contentId, delta);
    }

    @Override
    public void applyRatingChanged(UUID userId, UUID contentId, double oldRating, double newRating) {
        double delta = ContentRecommendationScorePolicy.calculateRatingChange(
                oldRating,
                newRating
        );
        applyDelta(userId, contentId, delta);
    }

    @Override
    public void applyRatingDeleted(UUID userId, UUID contentId, double oldRating) {
        double delta = ContentRecommendationScorePolicy.calculateRatingDeletion(oldRating);
        applyDelta(userId, contentId, delta);
    }

    private void applyDelta(
            UUID userId,
            UUID contentId,
            double delta
    ) {
        if (delta == 0.0) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        if (!content.getType().isPersonalizable()) {
            return;
        }

        updateGenrePreferences(user, contentId, delta);
        updateTagPreferences(user, contentId, delta);
    }

    private void updateGenrePreferences(
            User user,
            UUID contentId,
            double delta
    ) {
        List<ContentGenre> contentGenres =
                contentGenreRepository.findAllWithGenreByContentIdIn(List.of(contentId));
        if (contentGenres.isEmpty()) {
            return;
        }

        contentGenres.stream()
                .map(contentGenre -> contentGenre.getGenre().getId())
                .distinct()
                .sorted()
                .forEach(genreId -> contentPreferenceScoreRepository.addGenreScore(
                        user.getId(), genreId, delta));
    }

    private void updateTagPreferences(
            User user,
            UUID contentId,
            double delta
    ) {
        List<ContentTag> contentTags =
                contentTagRepository.findAllWithTagByContentIdIn(List.of(contentId));
        if (contentTags.isEmpty()) {
            return;
        }

        contentTags.stream()
                .map(contentTag -> contentTag.getTag().getId())
                .distinct()
                .sorted()
                .forEach(tagId -> contentPreferenceScoreRepository.addTagScore(
                        user.getId(), tagId, delta));
    }
}
