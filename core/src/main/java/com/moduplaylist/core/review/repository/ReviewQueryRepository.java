package com.moduplaylist.core.review.repository;

import com.moduplaylist.core.review.Review;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import static com.moduplaylist.core.content.repository.ContentQueryRepository.bytes;
import static com.moduplaylist.core.content.repository.ContentQueryRepository.uuid;

@Repository
@RequiredArgsConstructor
public class ReviewQueryRepository {
    private final EntityManager em;
    public record Page(List<Review> data, long totalCount) {}
    public Optional<UUID> contentId(UUID reviewId) {
        List<?> ids = em.createNativeQuery("SELECT content_id FROM reviews WHERE id=:id")
                .setParameter("id", bytes(reviewId)).getResultList();
        return ids.isEmpty() ? Optional.empty() : Optional.of(uuid(ids.get(0)));
    }

    /** 콘텐츠 잠금을 획득한 뒤 호출한다. FOR UPDATE로 최신 커밋 값을 읽는다. */
    @SuppressWarnings("unchecked")
    public List<BigDecimal> ratingsForUpdate(UUID contentId) {
        return em.createNativeQuery("SELECT rating FROM reviews WHERE content_id=:id FOR UPDATE")
                .setParameter("id", bytes(contentId)).getResultList();
    }

    @SuppressWarnings("unchecked")
    public Page search(UUID contentId, UUID userId, Instant cursor, UUID idAfter, boolean ascending, int limit) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        Map<String,Object> params = new HashMap<>();
        if (contentId != null) { where.append(" AND r.content_id=:content"); params.put("content", bytes(contentId)); }
        if (userId != null) { where.append(" AND r.user_id=:user"); params.put("user", bytes(userId)); }
        Query count = em.createNativeQuery("SELECT COUNT(*) FROM reviews r" + where);
        params.forEach(count::setParameter);
        long total = ((Number) count.getSingleResult()).longValue();
        String op = ascending ? ">" : "<";
        if (cursor != null) {
            where.append(" AND (r.created_at").append(op).append(":cursor OR (r.created_at=:cursor AND r.id")
                    .append(op).append(":after))");
            params.put("cursor", Timestamp.from(cursor));
            params.put("after", bytes(idAfter));
        }
        String order = ascending ? " ASC" : " DESC";
        Query data = em.createNativeQuery("SELECT r.* FROM reviews r" + where
                + " ORDER BY r.created_at" + order + ",r.id" + order, Review.class);
        params.forEach(data::setParameter);
        return new Page(data.setMaxResults(limit + 1).getResultList(), total);
    }
}
