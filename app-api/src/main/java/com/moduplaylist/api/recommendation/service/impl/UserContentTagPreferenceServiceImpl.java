package com.moduplaylist.api.recommendation.service.impl;

import com.moduplaylist.api.recommendation.service.UserContentTagPreferenceService;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.core.content.entity.ContentTag;
import com.moduplaylist.core.content.repository.ContentTagRepository;
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
public class UserContentTagPreferenceServiceImpl implements UserContentTagPreferenceService {

    private final ContentTagRepository contentTagRepository;
    private final ContentPreferenceScoreRepository contentPreferenceScoreRepository;

    @Override
    @Transactional
    public void createFromInitialPreferences(User user, Collection<UUID> contentIds) {
        List<ContentTag> contentTags =
                contentTagRepository.findAllWithTagByContentIdIn(contentIds);
        if (contentTags.isEmpty()) {
            return;
        }

        double initialWeight = ContentRecommendationScorePolicy.calculate(
                ContentActivityType.INITIAL_PREFERENCE
        );
        Map<UUID, Double> scoreByTagId = new LinkedHashMap<>();

        for (ContentTag contentTag : contentTags) {
            scoreByTagId.merge(contentTag.getTag().getId(), initialWeight, Double::sum);
        }

        scoreByTagId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> contentPreferenceScoreRepository.addTagScore(
                        user.getId(), entry.getKey(), entry.getValue()));
    }
}
