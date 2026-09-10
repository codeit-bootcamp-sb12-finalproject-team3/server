package com.moduplaylist.api.recommendation.service;

import com.moduplaylist.api.recommendation.dto.UserPreferenceResponse;
import com.moduplaylist.api.recommendation.dto.UserPreferenceCreateRequest;

import java.util.UUID;

public interface RecommendationService {

    UserPreferenceResponse createUserPreference(UUID userId, UserPreferenceCreateRequest request);

    UserPreferenceResponse findUserPreference(UUID userId);
}
