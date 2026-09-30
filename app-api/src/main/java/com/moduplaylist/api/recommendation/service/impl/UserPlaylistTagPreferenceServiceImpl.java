package com.moduplaylist.api.recommendation.service.impl;

import com.moduplaylist.api.recommendation.service.UserPlaylistTagPreferenceService;
import com.moduplaylist.core.content.entity.ContentTag;
import com.moduplaylist.core.content.repository.ContentTagRepository;
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
public class UserPlaylistTagPreferenceServiceImpl implements UserPlaylistTagPreferenceService {

    private final ContentTagRepository contentTagRepository;
    private final PlaylistPreferenceScoreRepository preferenceScoreRepository;

    @Override
    @Transactional
    public void createFromInitialPreferences(User user, Collection<UUID> contentIds) {
        List<ContentTag> contentTags = contentTagRepository.findAllWithTagByContentIdIn(contentIds);
        if (contentTags.isEmpty()) {
            return;
        }

        double initialWeight = PlaylistRecommendationScorePolicy.initialPreferenceWeight();
        Map<UUID, Double> scoresByTagId = new LinkedHashMap<>();

        for (ContentTag contentTag : contentTags) {
            scoresByTagId.merge(
                    contentTag.getTag().getId(),
                    initialWeight,
                    Double::sum
            );
        }

        scoresByTagId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> preferenceScoreRepository.addTagScore(
                        user.getId(), entry.getKey(), entry.getValue()));
    }
}
