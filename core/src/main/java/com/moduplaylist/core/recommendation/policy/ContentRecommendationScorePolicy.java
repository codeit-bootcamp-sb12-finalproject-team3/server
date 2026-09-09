package com.moduplaylist.core.recommendation.policy;

import com.moduplaylist.core.activity.enums.ContentActivityType;
import java.util.Objects;

public final class ContentRecommendationScorePolicy {

    private static final double INITIAL_PREFERENCE_WEIGHT = 0.5;
    private static final double CONTENT_LIKE_WEIGHT = 1.0;
    private static final double PLAYLIST_CONTENT_ADDED_WEIGHT = 0.3;
    private static final double WATCH_PARTY_JOINED_WEIGHT = 0.7;

    private static final double MIN_RATING = 0.0;
    private static final double MAX_RATING = 5.0;
    private static final double RATING_STEP = 0.5;

    private ContentRecommendationScorePolicy() {
    }

    public static double calculate(ContentActivityType activityType) {
        Objects.requireNonNull(activityType, "activityType must not be null");

        return switch (activityType) {
            case INITIAL_PREFERENCE -> INITIAL_PREFERENCE_WEIGHT;
            case CONTENT_LIKE -> CONTENT_LIKE_WEIGHT;
            case CONTENT_UNLIKE -> -CONTENT_LIKE_WEIGHT;
            case PLAYLIST_CONTENT_ADDED -> PLAYLIST_CONTENT_ADDED_WEIGHT;
            case PLAYLIST_CONTENT_REMOVED -> -PLAYLIST_CONTENT_ADDED_WEIGHT;
            case WATCH_PARTY_JOINED -> WATCH_PARTY_JOINED_WEIGHT;
            case CONTENT_VIEW -> 0.0;
            case CONTENT_RATING -> throw new IllegalArgumentException(
                    "CONTENT_RATING requires rating information"
            );
        };
    }

    public static double calculateRatingWeight(double rating) {
        validateRating(rating);

        if (rating <= 1.5) {
            return -1.0;
        }
        if (rating <= 2.5) {
            return -0.5;
        }
        if (rating <= 3.5) {
            return 0.0;
        }
        if (rating <= 4.5) {
            return 0.5;
        }
        return 1.0;
    }

    public static double calculateRatingChange(double oldRating, double newRating) {
        return calculateRatingWeight(newRating) - calculateRatingWeight(oldRating);
    }

    public static double calculateRatingDeletion(double oldRating) {
        return -calculateRatingWeight(oldRating);
    }

    private static void validateRating(double rating) {
        boolean outsideRange = !Double.isFinite(rating)
                || rating < MIN_RATING
                || rating > MAX_RATING;
        boolean invalidStep = rating % RATING_STEP != 0.0;

        if (outsideRange || invalidStep) {
            throw new IllegalArgumentException(
                    "rating must be between 0.0 and 5.0 in increments of 0.5: " + rating
            );
        }
    }
}
