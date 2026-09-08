package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.Content;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 아직 Entity가 없는 연결 테이블도 schema.sql의 관계를 기준으로 조회한다. */
@Repository
@RequiredArgsConstructor
public class ContentQueryRepository {
    private final EntityManager em;

    public record SearchResult(List<Content> contents, long totalCount) {}

    @SuppressWarnings("unchecked")
    public SearchResult search(ContentSearch request) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (request.type() != null) {
            where.append(" AND c.type = :type");
            params.put("type", request.type().equals("tvSeries") ? "tvSeason" : request.type());
        } else {
            where.append(" AND c.type <> 'tvSeries'");
        }
        if (request.genreId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM content_genres g WHERE g.content_id=c.id AND g.genre_id=:genre)");
            params.put("genre", bytes(request.genreId()));
        }
        if (request.sportType() != null) {
            where.append(" AND c.sport_type=:sport");
            params.put("sport", request.sportType());
        }
        if (request.likedByUserId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM content_likes l WHERE l.content_id=c.id AND l.user_id=:likedBy)");
            params.put("likedBy", bytes(request.likedByUserId()));
        }
        if (request.keyword() != null && !request.keyword().isBlank()) {
            where.append("""
                 AND (LOWER(c.title) LIKE :keyword ESCAPE '!'
                   OR LOWER(c.description) LIKE :keyword ESCAPE '!'
                   OR LOWER(c.type) LIKE :keyword ESCAPE '!'
                   OR LOWER(CAST(c.metadata AS CHAR)) LIKE :keyword ESCAPE '!'
                   OR EXISTS (SELECT 1 FROM content_tags ct JOIN tags t ON t.id=ct.tag_id
                              WHERE ct.content_id=c.id AND LOWER(t.name) LIKE :keyword ESCAPE '!')
                   OR EXISTS (SELECT 1 FROM content_genres cg JOIN genres g ON g.id=cg.genre_id
                              WHERE cg.content_id=c.id AND LOWER(g.name) LIKE :keyword ESCAPE '!')
                   OR EXISTS (SELECT 1 FROM content_casts cc WHERE cc.content_id=c.id
                              AND LOWER(cc.name) LIKE :keyword ESCAPE '!'))
                """);
            params.put("keyword", "%" + request.keyword().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }
        Query count = em.createNativeQuery("SELECT COUNT(*) FROM contents c" + where);
        params.forEach(count::setParameter);
        long total = ((Number) count.getSingleResult()).longValue();
        String sort = request.ratingSort() ? "c.average_rating" : "c.created_at";
        String operator = request.ascending() ? ">" : "<";
        if (request.idAfter() != null) {
            where.append(" AND (").append(sort).append(operator).append(":cursor OR (")
                    .append(sort).append("=:cursor AND c.id").append(operator).append(":after))");
            params.put("cursor", request.ratingSort() ? request.cursorRating() : Timestamp.from(request.cursorTime()));
            params.put("after", bytes(request.idAfter()));
        }
        String direction = request.ascending() ? " ASC" : " DESC";
        Query query = em.createNativeQuery("SELECT c.* FROM contents c" + where
                + " ORDER BY " + sort + direction + ", c.id" + direction, Content.class);
        params.forEach(query::setParameter);
        return new SearchResult(query.setMaxResults(request.limit() + 1).getResultList(), total);
    }

    public static byte[] bytes(UUID id) {
        return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array();
    }

    public static UUID uuid(Object value) {
        if (value instanceof UUID id) return id;
        ByteBuffer buffer = ByteBuffer.wrap((byte[]) value);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
