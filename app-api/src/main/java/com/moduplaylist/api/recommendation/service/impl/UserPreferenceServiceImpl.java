package com.moduplaylist.api.recommendation.service.impl;

import com.moduplaylist.api.recommendation.dto.UserPreferenceCreateRequest;
import com.moduplaylist.api.recommendation.dto.UserPreferenceResponse;
import com.moduplaylist.api.recommendation.service.UserContentGenrePreferenceService;
import com.moduplaylist.api.recommendation.service.UserContentTagPreferenceService;
import com.moduplaylist.api.recommendation.service.UserPreferenceService;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.recommendation.entity.UserPreferenceContent;
import com.moduplaylist.core.recommendation.exception.PreferenceAlreadyExistsException;
import com.moduplaylist.core.recommendation.exception.PreferenceNotFoundException;
import com.moduplaylist.core.recommendation.repository.UserPreferenceContentRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceContentRepository userPreferenceContentRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final UserContentTagPreferenceService userContentTagPreferenceService;
    private final UserContentGenrePreferenceService userContentGenrePreferenceService;

    @Override
    @Transactional
    public UserPreferenceResponse createUserPreference(
            UUID userId,
            UserPreferenceCreateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (userPreferenceContentRepository.existsByUser_Id(userId)) {
            throw new PreferenceAlreadyExistsException(userId);
        }

        List<UUID> contentIds = request.getContentIds();
        List<Content> contents = contentRepository.findAllById(contentIds);
        Map<UUID, Content> contentById = indexById(contents);

        for (UUID contentId : contentIds) {
            if (!contentById.containsKey(contentId)) {
                throw new ContentNotFoundException(contentId);
            }
        }

        List<UserPreferenceContent> preferences = contentIds.stream()
                .map(contentById::get)
                .map(content -> UserPreferenceContent.create(user, content))
                .toList();
        userPreferenceContentRepository.saveAll(preferences);
        userContentTagPreferenceService.createFromInitialPreferences(user, contentIds);
        userContentGenrePreferenceService.createFromInitialPreferences(user, contentIds);

        return UserPreferenceResponse.builder()
                .contentIds(contentIds)
                .build();
    }

    private Map<UUID, Content> indexById(List<Content> contents) {
        Map<UUID, Content> contentById = new HashMap<>();
        contents.forEach(content -> contentById.put(content.getId(), content));
        return contentById;
    }

    @Override
    @Transactional(readOnly = true)
    public UserPreferenceResponse findUserPreference(UUID userId) {
        List<UserPreferenceContent> preferences =
                userPreferenceContentRepository.findAllByUser_Id(userId);

        if (preferences.isEmpty()) {
            throw new PreferenceNotFoundException(userId);
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
