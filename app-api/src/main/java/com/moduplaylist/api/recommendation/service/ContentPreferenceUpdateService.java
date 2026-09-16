package com.moduplaylist.api.recommendation.service;

import com.moduplaylist.core.activity.enums.ContentActivityType;

import java.util.UUID;

public interface ContentPreferenceUpdateService {

    void applyActivity(
            UUID userId,
            UUID contentId,
            ContentActivityType activityType
    );

    void applyRatingChanged(
            UUID userId,
            UUID contentId,
            double oldRating,
            double newRating
    );

    void applyRatingDeleted(
            UUID userId,
            UUID contentId,
            double oldRating
    );
}
