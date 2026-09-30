package com.moduplaylist.infrastructure.recommendation;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mopl.recommendation")
public class RecommendationProperties {

    private int searchLimit = 100;
    private Duration cacheTtl = Duration.ofHours(48);

    public int getSearchLimit() {
        return searchLimit;
    }

    public void setSearchLimit(int searchLimit) {
        if (searchLimit < 1 || searchLimit > 10_000) {
            throw new IllegalArgumentException("추천 검색 개수는 1부터 10000 사이여야 합니다.");
        }
        this.searchLimit = searchLimit;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        if (cacheTtl == null || cacheTtl.isZero() || cacheTtl.isNegative()) {
            throw new IllegalArgumentException("추천 캐시 TTL은 0보다 커야 합니다.");
        }
        this.cacheTtl = cacheTtl;
    }
}
