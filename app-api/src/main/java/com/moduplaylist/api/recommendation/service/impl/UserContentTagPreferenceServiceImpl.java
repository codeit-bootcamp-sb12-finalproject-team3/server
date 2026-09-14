package com.moduplaylist.api.recommendation.service.impl;

import com.moduplaylist.api.recommendation.service.UserContentTagPreferenceService;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.core.content.entity.ContentTag;
import com.moduplaylist.core.content.entity.Tag;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.core.recommendation.entity.UserContentTagPreference;
import com.moduplaylist.core.recommendation.policy.ContentRecommendationScorePolicy;
import com.moduplaylist.core.recommendation.repository.UserContentTagPreferenceRepository;
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
public class UserContentTagPreferenceServiceImpl implements UserContentTagPreferenceService {

    private final ContentTagRepository contentTagRepository;
    private final UserContentTagPreferenceRepository userContentTagPreferenceRepository;

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
        Map<UUID, Tag> tagById = new LinkedHashMap<>();
        Map<UUID, Double> scoreByTagId = new LinkedHashMap<>();

        for (ContentTag contentTag : contentTags) {
            Tag tag = contentTag.getTag();
            tagById.putIfAbsent(tag.getId(), tag);
            scoreByTagId.merge(tag.getId(), initialWeight, Double::sum);
        }

        Instant scoreUpdatedAt = Instant.now();
        List<UserContentTagPreference> tagPreferences = scoreByTagId.entrySet().stream()
                .map(entry -> UserContentTagPreference.create(
                        user,
                        tagById.get(entry.getKey()),
                        entry.getValue(),
                        scoreUpdatedAt
                ))
                .toList();

        userContentTagPreferenceRepository.saveAll(tagPreferences);
    }
}
