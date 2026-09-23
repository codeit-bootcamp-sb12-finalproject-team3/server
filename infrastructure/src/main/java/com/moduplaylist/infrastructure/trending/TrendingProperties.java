package com.moduplaylist.infrastructure.trending;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mopl.trending")
public class TrendingProperties {

    private Duration processedEventTtl = Duration.ofDays(7);
    private final Weight weight = new Weight();

    public Duration getProcessedEventTtl() {
        return processedEventTtl;
    }

    public void setProcessedEventTtl(Duration processedEventTtl) {
        if (processedEventTtl == null
                || processedEventTtl.isZero()
                || processedEventTtl.isNegative()) {
            throw new IllegalArgumentException("트렌딩 이벤트 중복 방지 TTL은 0보다 커야 합니다.");
        }
        this.processedEventTtl = processedEventTtl;
    }

    public Weight getWeight() {
        return weight;
    }

    public static class Weight {

        private double contentView = 0.5;
        private double contentLike = 2.0;
        private double rating = 1.5;
        private double playlistContent = 1.0;
        private double watchPartyParticipation = 1.0;

        public double getContentView() {
            return contentView;
        }

        public void setContentView(double contentView) {
            this.contentView = requirePositive(contentView, "콘텐츠 조회 가중치");
        }

        public double getContentLike() {
            return contentLike;
        }

        public void setContentLike(double contentLike) {
            this.contentLike = requirePositive(contentLike, "콘텐츠 좋아요 가중치");
        }

        public double getRating() {
            return rating;
        }

        public void setRating(double rating) {
            this.rating = requirePositive(rating, "콘텐츠 평점 가중치");
        }

        public double getPlaylistContent() {
            return playlistContent;
        }

        public void setPlaylistContent(double playlistContent) {
            this.playlistContent = requirePositive(
                    playlistContent,
                    "플레이리스트 콘텐츠 가중치"
            );
        }

        public double getWatchPartyParticipation() {
            return watchPartyParticipation;
        }

        public void setWatchPartyParticipation(double watchPartyParticipation) {
            this.watchPartyParticipation = requirePositive(
                    watchPartyParticipation,
                    "같이보기 참가 가중치"
            );
        }

        private static double requirePositive(double value, String name) {
            if (!Double.isFinite(value) || value <= 0.0) {
                throw new IllegalArgumentException(name + "는 0보다 큰 유한한 값이어야 합니다.");
            }
            return value;
        }
    }
}
