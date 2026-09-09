package com.moduplaylist.api.recommendation.service;

import com.moduplaylist.api.recommendation.dto.UserPreferenceResponse;
import java.util.UUID;

public interface RecommendationService {

    UserPreferenceResponse findUserPreference(UUID userId);
}
