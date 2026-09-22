package com.moduplaylist.api.recommendation.service;

import com.moduplaylist.core.activity.enums.ContentActivityType;

import java.util.UUID;

public interface ContentPreferenceUpdateService {

    double applyActivity(
            UUID userId,
            UUID contentId,
            ContentActivityType activityType
    );

    double applyRatingCreated(
            UUID userId,
            UUID contentId,
            double rating
    );

    double applyRatingChanged(
            UUID userId,
            UUID contentId,
            double oldRating,
            double newRating
    );

    double applyRatingDeleted(
            UUID userId,
            UUID contentId,
            double oldRating
    );
}
