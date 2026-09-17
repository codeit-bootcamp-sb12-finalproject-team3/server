package com.moduplaylist.api.recommendation.service.impl;

import com.moduplaylist.api.recommendation.service.ContentPreferenceUpdateService;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentGenre;
import com.moduplaylist.core.content.entity.ContentTag;
import com.moduplaylist.core.content.entity.Genre;
import com.moduplaylist.core.content.entity.Tag;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.core.recommendation.entity.UserContentGenrePreference;
import com.moduplaylist.core.recommendation.entity.UserContentTagPreference;
import com.moduplaylist.core.recommendation.policy.ContentRecommendationScorePolicy;
import com.moduplaylist.core.recommendation.repository.UserContentGenrePreferenceRepository;
import com.moduplaylist.core.recommendation.repository.UserContentTagPreferenceRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final UserContentGenrePreferenceRepository userContentGenrePreferenceRepository;
    private final UserContentTagPreferenceRepository userContentTagPreferenceRepository;

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

        Instant scoreUpdatedAt = Instant.now();
        updateGenrePreferences(user, contentId, delta, scoreUpdatedAt);
        updateTagPreferences(user, contentId, delta, scoreUpdatedAt);
    }

    private void updateGenrePreferences(
            User user,
            UUID contentId,
            double delta,
            Instant scoreUpdatedAt
    ) {
        List<ContentGenre> contentGenres =
                contentGenreRepository.findAllWithGenreByContentIdIn(List.of(contentId));
        if (contentGenres.isEmpty()) {
            return;
        }

        Map<UUID, Genre> genreById = new LinkedHashMap<>();
        contentGenres.forEach(contentGenre -> {
            Genre genre = contentGenre.getGenre();
            genreById.putIfAbsent(genre.getId(), genre);
        });

        List<UserContentGenrePreference> existingPreferences =
                userContentGenrePreferenceRepository.findAllWithGenreByUserIdAndGenreIdIn(
                        user.getId(),
                        genreById.keySet()
                );
        Map<UUID, UserContentGenrePreference> preferenceByGenreId = new LinkedHashMap<>();
        existingPreferences.forEach(preference ->
                preferenceByGenreId.put(preference.getGenre().getId(), preference));
        existingPreferences.forEach(preference -> preference.addScore(delta));

        Set<UUID> existingGenreIds = preferenceByGenreId.keySet();
        List<UserContentGenrePreference> newPreferences = genreById.entrySet().stream()
                .filter(entry -> !existingGenreIds.contains(entry.getKey()))
                .map(entry -> UserContentGenrePreference.create(
                        user,
                        entry.getValue(),
                        delta,
                        scoreUpdatedAt
                ))
                .toList();
        if (!newPreferences.isEmpty()) {
            userContentGenrePreferenceRepository.saveAll(newPreferences);
        }
    }

    private void updateTagPreferences(
            User user,
            UUID contentId,
            double delta,
            Instant scoreUpdatedAt
    ) {
        List<ContentTag> contentTags =
                contentTagRepository.findAllWithTagByContentIdIn(List.of(contentId));
        if (contentTags.isEmpty()) {
            return;
        }

        Map<UUID, Tag> tagById = new LinkedHashMap<>();
        contentTags.forEach(contentTag -> {
            Tag tag = contentTag.getTag();
            tagById.putIfAbsent(tag.getId(), tag);
        });

        List<UserContentTagPreference> existingPreferences =
                userContentTagPreferenceRepository.findAllWithTagByUserIdAndTagIdIn(
                        user.getId(),
                        tagById.keySet()
                );
        Map<UUID, UserContentTagPreference> preferenceByTagId = new LinkedHashMap<>();
        existingPreferences.forEach(preference ->
                preferenceByTagId.put(preference.getTag().getId(), preference));
        existingPreferences.forEach(preference -> preference.addScore(delta));

        Set<UUID> existingTagIds = preferenceByTagId.keySet();
        List<UserContentTagPreference> newPreferences = tagById.entrySet().stream()
                .filter(entry -> !existingTagIds.contains(entry.getKey()))
                .map(entry -> UserContentTagPreference.create(
                        user,
                        entry.getValue(),
                        delta,
                        scoreUpdatedAt
                ))
                .toList();
        if (!newPreferences.isEmpty()) {
            userContentTagPreferenceRepository.saveAll(newPreferences);
        }
    }
}
