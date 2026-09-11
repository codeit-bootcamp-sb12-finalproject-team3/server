package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 아직 Entity가 없는 연결 테이블도 schema.sql의 관계를 기준으로 조회한다. */
@Repository
@RequiredArgsConstructor
public class ContentQueryRepository {
    private final EntityManager em;

    @Getter
    @RequiredArgsConstructor
    public static class SearchResult {
        private final List<Content> contents;
        private final long totalCount;
        private final boolean hasNext;
    }

    @SuppressWarnings("unchecked")
    public SearchResult search(ContentSearch request) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (request.getType() != null) {
            where.append(" AND c.type = :type");
            ContentType queryType = request.getType() == ContentType.TV_SERIES
                    ? ContentType.TV_SEASON : request.getType();
            params.put("type", queryType.getValue());
        } else {
            // 탐색 목록에서는 시리즈 컨테이너 대신 개별 시즌을 노출한다.
            where.append(" AND c.type <> :excludedType");
            params.put("excludedType", ContentType.TV_SERIES.getValue());
        }
        if (request.getGenreId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM content_genres g WHERE g.content_id=c.id AND g.genre_id=:genre)");
            params.put("genre", bytes(request.getGenreId()));
        }
        if (request.getSportType() != null) {
            where.append(" AND c.sport_type=:sport");
            params.put("sport", request.getSportType());
        }
        if (request.getLikedByUserId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM content_likes l WHERE l.content_id=c.id AND l.user_id=:likedBy)");
            params.put("likedBy", bytes(request.getLikedByUserId()));
        }
        Query count = em.createNativeQuery("SELECT COUNT(*) FROM contents c" + where);
        params.forEach(count::setParameter);
        long total = ((Number) count.getSingleResult()).longValue();
        boolean ratingSort = request.getSort() == ContentSearch.Sort.RATING;
        if (request.getCursorId() != null) {
            appendCursorCondition(where, params, request, ratingSort);
        }
        String orderBy = ratingSort
                ? "c.average_rating DESC, c.title ASC, c.id ASC"
                : "c.release_date IS NULL ASC, c.release_date DESC, "
                        + "c.average_rating DESC, c.title ASC, c.id ASC";
        Query query = em.createNativeQuery("SELECT c.* FROM contents c" + where
                + " ORDER BY " + orderBy, Content.class);
        params.forEach(query::setParameter);
        List<Content> fetched = query.setMaxResults(request.getLimit() + 1).getResultList();
        boolean hasNext = fetched.size() > request.getLimit();
        List<Content> contents = hasNext
                ? List.copyOf(fetched.subList(0, request.getLimit())) : fetched;
        return new SearchResult(contents, total, hasNext);
    }

    private void appendCursorCondition(StringBuilder where, Map<String, Object> params,
            ContentSearch request, boolean ratingSort) {
        String cursorTitle = "(SELECT cursor_content.title FROM contents cursor_content WHERE cursor_content.id=:after)";
        String titleAndIdAfter = "(c.title > " + cursorTitle
                + " OR (c.title = " + cursorTitle + " AND c.id > :after))";
        params.put("after", bytes(request.getCursorId()));
        params.put("cursorRating", request.getCursorRating());

        String ratingAndTitleAfter = "(c.average_rating < :cursorRating OR "
                + "(c.average_rating = :cursorRating AND " + titleAndIdAfter + "))";

        if (ratingSort) {
            where.append(" AND ").append(ratingAndTitleAfter);
        } else if (request.getCursorReleaseDate() == null) {
            where.append(" AND c.release_date IS NULL AND ").append(ratingAndTitleAfter);
        } else {
            where.append(" AND (c.release_date < :cursorReleaseDate OR c.release_date IS NULL OR ")
                    .append("(c.release_date = :cursorReleaseDate AND ")
                    .append(ratingAndTitleAfter).append("))");
            params.put("cursorReleaseDate", request.getCursorReleaseDate());
        }
    }

    private byte[] bytes(UUID id) {
        return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array();
    }
}
