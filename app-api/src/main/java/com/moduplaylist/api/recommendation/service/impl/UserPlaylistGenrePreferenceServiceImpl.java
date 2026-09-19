package com.moduplaylist.api.recommendation.service.impl;

import com.moduplaylist.api.recommendation.service.UserPlaylistGenrePreferenceService;
import com.moduplaylist.core.content.entity.ContentGenre;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.recommendation.policy.PlaylistRecommendationScorePolicy;
import com.moduplaylist.core.recommendation.repository.PlaylistPreferenceScoreRepository;
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
public class UserPlaylistGenrePreferenceServiceImpl implements UserPlaylistGenrePreferenceService {

    private final ContentGenreRepository contentGenreRepository;
    private final PlaylistPreferenceScoreRepository preferenceScoreRepository;

    @Override
    @Transactional
    public void createFromInitialPreferences(User user, Collection<UUID> contentIds) {
        List<ContentGenre> contentGenres =
                contentGenreRepository.findAllWithGenreByContentIdIn(contentIds);
        if (contentGenres.isEmpty()) {
            return;
        }

        double initialWeight = PlaylistRecommendationScorePolicy.initialPreferenceWeight();
        Map<UUID, Double> scoresByGenreId = new LinkedHashMap<>();

        for (ContentGenre contentGenre : contentGenres) {
            scoresByGenreId.merge(
                    contentGenre.getGenre().getId(),
                    initialWeight,
                    Double::sum
            );
        }

        scoresByGenreId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> preferenceScoreRepository.addGenreScore(
                        user.getId(), entry.getKey(), entry.getValue()));
    }
}
