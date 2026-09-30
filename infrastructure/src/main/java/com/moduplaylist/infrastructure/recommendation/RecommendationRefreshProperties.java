package com.moduplaylist.infrastructure.recommendation;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mopl.recommendation.refresh")
public class RecommendationRefreshProperties {

    private double threshold = 3.0;
    private Duration lockTtl = Duration.ofMinutes(5);

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        if (!Double.isFinite(threshold) || threshold <= 0.0) {
            throw new IllegalArgumentException("추천 갱신 임계치는 0보다 큰 유한한 값이어야 합니다.");
        }
        this.threshold = threshold;
    }

    public Duration getLockTtl() {
        return lockTtl;
    }

    public void setLockTtl(Duration lockTtl) {
        if (lockTtl == null || lockTtl.isZero() || lockTtl.isNegative()) {
            throw new IllegalArgumentException("추천 갱신 lock TTL은 0보다 커야 합니다.");
        }
        this.lockTtl = lockTtl;
    }
}
