package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.entity.WatchParty;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class WatchPartyQueryRepository {

    private final EntityManager em;

    @Getter
    @RequiredArgsConstructor
    public static class SearchResult {
        private final List<WatchParty> watchParties;
        private final long totalCount;
        private final boolean hasNext;
    }

    public SearchResult search(WatchPartySearch request) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" where 1=1");

        if (request.getStatusEqual() != null) {
            where.append(" and w.status = :status");
            params.put("status", request.getStatusEqual());
        }
        if (request.getContentIdEqual() != null) {
            where.append(" and w.contentId = :contentId");
            params.put("contentId", request.getContentIdEqual());
        }

        TypedQuery<Long> countQuery = em.createQuery(
                "select count(w) from WatchParty w" + where, Long.class);
        params.forEach(countQuery::setParameter);
        long totalCount = countQuery.getSingleResult();

        String compare = request.isAscending() ? ">" : "<";
        StringBuilder listWhere = new StringBuilder(where);
        if (request.getCursorScheduledAt() != null && request.getCursorId() != null) {
            listWhere.append(" and (w.scheduledAt ").append(compare).append(" :cursorScheduledAt")
                    .append(" or (w.scheduledAt = :cursorScheduledAt and w.id ").append(compare).append(" :cursorId))");
            params.put("cursorScheduledAt", request.getCursorScheduledAt());
            params.put("cursorId", request.getCursorId());
        }

        String direction = request.isAscending() ? "asc" : "desc";
        String jpql = "select w from WatchParty w" + listWhere
                + " order by w.scheduledAt " + direction + ", w.id " + direction;

        TypedQuery<WatchParty> query = em.createQuery(jpql, WatchParty.class);
        params.forEach(query::setParameter);

        List<WatchParty> fetched = query.setMaxResults(request.getLimit() + 1).getResultList();
        boolean hasNext = fetched.size() > request.getLimit();
        List<WatchParty> result = hasNext ? fetched.subList(0, request.getLimit()) : fetched;

        return new SearchResult(result, totalCount, hasNext);
    }
}