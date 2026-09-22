package com.moduplaylist.infrastructure.redis.recommendation;

import java.util.List;
import java.util.UUID;

public record RecommendationCachePage(List<UUID> contentIds, long totalCount) {
}
