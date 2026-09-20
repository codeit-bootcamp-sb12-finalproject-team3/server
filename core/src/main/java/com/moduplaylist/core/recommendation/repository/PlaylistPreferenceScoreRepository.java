package com.moduplaylist.core.recommendation.repository;

import com.github.f4b6a3.uuid.UuidCreator;
import java.nio.ByteBuffer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaylistPreferenceScoreRepository {

    private static final String TAG_SCORE_UPSERT = """
            INSERT INTO user_playlist_tag_preferences(id, user_id, tag_id, score, updated_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE
                score = score + ?,
                updated_at = CURRENT_TIMESTAMP(6)
            """;

    private static final String GENRE_SCORE_UPSERT = """
            INSERT INTO user_playlist_genre_preferences(id, user_id, genre_id, score, updated_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE
                score = score + ?,
                updated_at = CURRENT_TIMESTAMP(6)
            """;

    private final JdbcTemplate jdbcTemplate;

    public void addTagScore(UUID userId, UUID tagId, double delta) {
        jdbcTemplate.update(TAG_SCORE_UPSERT,
                bytes(UuidCreator.getTimeOrderedEpoch()), bytes(userId), bytes(tagId), delta, delta);
    }

    public void addGenreScore(UUID userId, UUID genreId, double delta) {
        jdbcTemplate.update(GENRE_SCORE_UPSERT,
                bytes(UuidCreator.getTimeOrderedEpoch()), bytes(userId), bytes(genreId), delta, delta);
    }

    private byte[] bytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }
}
