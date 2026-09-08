package com.moduplaylist.api.recommendation.service.impl;

import com.moduplaylist.api.recommendation.dto.UserPreferenceResponse;
import com.moduplaylist.api.recommendation.service.RecommendationService;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.recommendation.entity.UserPreferenceContent;
import com.moduplaylist.core.recommendation.repository.UserPreferenceContentRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final UserPreferenceContentRepository userPreferenceContentRepository;

    @Override
    @Transactional(readOnly = true)
    public UserPreferenceResponse findUserPreference(UUID userId) {
        List<UserPreferenceContent> preferences =
                userPreferenceContentRepository.findAllByUser_Id(userId);

        if (preferences.isEmpty()) {
            throw new BaseException(ErrorCode.PREFERENCE_NOT_FOUND);
        }

        List<UUID> contentIds = preferences.stream()
                .map(UserPreferenceContent::getContent)
                .map(Content::getId)
                .toList();

        return UserPreferenceResponse.builder()
                .contentIds(contentIds)
                .build();
    }
}
