package com.moduplaylist.infrastructure.trending;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mopl.trending")
public class TrendingProperties {

    private Duration processedEventTtl = Duration.ofDays(7);
    private int windowHours = 24;
    private Duration bucketTtl = Duration.ofHours(26);
    private Duration resultCacheTtl = Duration.ofSeconds(30);
    private final Weight weight = new Weight();

    public Duration getProcessedEventTtl() {
        return processedEventTtl;
    }

    public void setProcessedEventTtl(Duration processedEventTtl) {
        this.processedEventTtl = requirePositive(
                processedEventTtl,
                "트렌딩 이벤트 중복 방지 TTL"
        );
    }

    public int getWindowHours() {
        return windowHours;
    }

    public void setWindowHours(int windowHours) {
        if (windowHours < 1) {
            throw new IllegalArgumentException("트렌딩 집계 시간은 1시간 이상이어야 합니다.");
        }
        this.windowHours = windowHours;
    }

    public Duration getBucketTtl() {
        return bucketTtl;
    }

    public void setBucketTtl(Duration bucketTtl) {
        this.bucketTtl = requirePositive(bucketTtl, "트렌딩 시간 버킷 TTL");
    }

    public Duration getResultCacheTtl() {
        return resultCacheTtl;
    }

    public void setResultCacheTtl(Duration resultCacheTtl) {
        this.resultCacheTtl = requirePositive(resultCacheTtl, "트렌딩 결과 캐시 TTL");
    }

    public Weight getWeight() {
        return weight;
    }

    private static Duration requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + "은 0보다 커야 합니다.");
        }
        return value;
    }

    public static class Weight {

        private double contentView = 0.1;
        private double contentLike = 1.5;
        private double rating = 1.0;
        private double playlistContent = 0.7;
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
