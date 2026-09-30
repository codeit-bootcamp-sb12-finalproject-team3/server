package com.moduplaylist.core.recommendation.repository;

import java.nio.ByteBuffer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RecommendationProcessedEventRepository {

    private static final String MARK_PROCESSED = """
            INSERT IGNORE INTO recommendation_processed_events(event_id, processed_at)
            VALUES (?, CURRENT_TIMESTAMP(6))
            """;

    private final JdbcTemplate jdbcTemplate;

    public boolean tryMarkProcessed(UUID eventId) {
        return jdbcTemplate.update(MARK_PROCESSED, bytes(eventId)) == 1;
    }

    private byte[] bytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }
}
