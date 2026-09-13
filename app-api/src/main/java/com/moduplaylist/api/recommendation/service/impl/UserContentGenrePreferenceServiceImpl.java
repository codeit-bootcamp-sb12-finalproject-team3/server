package com.moduplaylist.api.recommendation.service.impl;

import com.moduplaylist.api.recommendation.service.UserContentGenrePreferenceService;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.core.content.entity.ContentGenre;
import com.moduplaylist.core.content.entity.Genre;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.recommendation.entity.UserContentGenrePreference;
import com.moduplaylist.core.recommendation.policy.ContentRecommendationScorePolicy;
import com.moduplaylist.core.recommendation.repository.UserContentGenrePreferenceRepository;
import com.moduplaylist.core.user.entity.User;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserContentGenrePreferenceServiceImpl
        implements UserContentGenrePreferenceService {

    private final ContentGenreRepository contentGenreRepository;
    private final UserContentGenrePreferenceRepository userContentGenrePreferenceRepository;

    @Override
    @Transactional
    public void createFromInitialPreferences(User user, Collection<UUID> contentIds) {
        List<ContentGenre> contentGenres =
                contentGenreRepository.findAllWithGenreByContentIdIn(contentIds);
        if (contentGenres.isEmpty()) {
            return;
        }

        double initialWeight = ContentRecommendationScorePolicy.calculate(
                ContentActivityType.INITIAL_PREFERENCE
        );
        Map<UUID, Genre> genreById = new LinkedHashMap<>();
        Map<UUID, Double> scoreByGenreId = new LinkedHashMap<>();

        for (ContentGenre contentGenre : contentGenres) {
            Genre genre = contentGenre.getGenre();
            genreById.putIfAbsent(genre.getId(), genre);
            scoreByGenreId.merge(genre.getId(), initialWeight, Double::sum);
        }

        Instant scoreUpdatedAt = Instant.now();
        List<UserContentGenrePreference> genrePreferences = scoreByGenreId.entrySet().stream()
                .map(entry -> UserContentGenrePreference.create(
                        user,
                        genreById.get(entry.getKey()),
                        entry.getValue(),
                        scoreUpdatedAt
                ))
                .toList();

        userContentGenrePreferenceRepository.saveAll(genrePreferences);
    }
}
