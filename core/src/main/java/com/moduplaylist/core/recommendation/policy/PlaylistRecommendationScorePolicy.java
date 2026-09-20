package com.moduplaylist.core.recommendation.policy;

import com.moduplaylist.core.activity.enums.PlaylistActivityType;

import java.util.Objects;

public final class PlaylistRecommendationScorePolicy {

    private static final double INITIAL_PREFERENCE_WEIGHT = 0.5;
    private static final double PLAYLIST_SUBSCRIBED_WEIGHT = 1.0;

    private PlaylistRecommendationScorePolicy() {
    }

    public static double initialPreferenceWeight() {
        return INITIAL_PREFERENCE_WEIGHT;
    }

    public static double calculate(PlaylistActivityType activityType) {
        Objects.requireNonNull(activityType, "activityType must not be null");

        return switch (activityType) {
            case PLAYLIST_SUBSCRIBED -> PLAYLIST_SUBSCRIBED_WEIGHT;
            case PLAYLIST_UNSUBSCRIBED -> -PLAYLIST_SUBSCRIBED_WEIGHT;
        };
    }

}
