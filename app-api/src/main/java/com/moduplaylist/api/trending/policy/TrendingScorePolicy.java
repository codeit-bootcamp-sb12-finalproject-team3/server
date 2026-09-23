package com.moduplaylist.api.trending.policy;

import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.infrastructure.kafka.event.ContentActivityKafkaEvent;
import com.moduplaylist.infrastructure.trending.TrendingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrendingScorePolicy {

    private final TrendingProperties properties;

    public double calculate(ContentActivityKafkaEvent event) {
        ContentActivityType eventType = event.eventType();
        TrendingProperties.Weight weight = properties.getWeight();

        return switch (eventType) {
            case CONTENT_VIEW -> weight.getContentView();
            case CONTENT_LIKE -> weight.getContentLike();
            case CONTENT_UNLIKE -> -weight.getContentLike();
            case PLAYLIST_CONTENT_ADDED -> weight.getPlaylistContent();
            case PLAYLIST_CONTENT_REMOVED -> -weight.getPlaylistContent();
            case CONTENT_RATING -> calculateRatingContribution(event, weight.getRating());
            case INITIAL_PREFERENCE, WATCH_PARTY_JOINED -> 0.0;
        };
    }

    public double watchPartyParticipation() {
        return properties.getWeight().getWatchPartyParticipation();
    }

    private double calculateRatingContribution(
            ContentActivityKafkaEvent event,
            double ratingWeight
    ) {
        if (event.oldRating() == null && event.newRating() != null) {
            return ratingWeight;
        }
        if (event.oldRating() != null && event.newRating() == null) {
            return -ratingWeight;
        }

        // 트렌딩은 평점의 높낮이가 아닌 평점 활동의 존재를 반영하므로 수정 시 총 기여도는 유지한다.
        return 0.0;
    }
}
