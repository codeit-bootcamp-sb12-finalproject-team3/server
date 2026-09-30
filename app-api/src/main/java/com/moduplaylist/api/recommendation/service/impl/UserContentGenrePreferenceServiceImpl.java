package com.moduplaylist.api.recommendation.service.impl;

import com.moduplaylist.api.recommendation.service.UserContentGenrePreferenceService;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.core.content.entity.ContentGenre;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.recommendation.policy.ContentRecommendationScorePolicy;
import com.moduplaylist.core.recommendation.repository.ContentPreferenceScoreRepository;
import com.moduplaylist.core.user.entity.User;
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
    private final ContentPreferenceScoreRepository contentPreferenceScoreRepository;

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
        Map<UUID, Double> scoreByGenreId = new LinkedHashMap<>();

        for (ContentGenre contentGenre : contentGenres) {
            scoreByGenreId.merge(contentGenre.getGenre().getId(), initialWeight, Double::sum);
        }

        scoreByGenreId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> contentPreferenceScoreRepository.addGenreScore(
                        user.getId(), entry.getKey(), entry.getValue()));
    }
}
