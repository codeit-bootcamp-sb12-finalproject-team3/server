package com.moduplaylist.api.recommendation.service;

import com.moduplaylist.api.recommendation.dto.UserPreferenceCreateRequest;
import com.moduplaylist.api.recommendation.dto.UserPreferenceResponse;
import java.util.UUID;

public interface UserPreferenceService {

    UserPreferenceResponse createUserPreference(
            UUID userId,
            UserPreferenceCreateRequest request
    );

    UserPreferenceResponse findUserPreference(UUID userId);
}
