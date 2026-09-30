package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Repository
@RequiredArgsConstructor
public class WatchPartyQueryRepository {

    private final EntityManager em;

    // 목록에서 "지금 들어갈 수 있는 파티" = 진행 중 + 예정 (종료 제외)
    private static final List<WatchPartyStatus> ACTIVE_STATUSES =
            List.of(WatchPartyStatus.LIVE, WatchPartyStatus.SCHEDULED);

    @Getter
    public static class SearchResult {
        private final List<WatchParty> watchParties;
        private final long totalCount;
        private final boolean hasNext;
        // 인기순일 때만 채움: 파티 id → JOINED 참가자 수 (① 순위표에서 센 값을 서비스가 재사용)
        private final Map<UUID, Long> participantCounts;

        // 기존 경로(기본·콘텐츠)용 — 참가자 수는 서비스가 따로 배치 조회
        public SearchResult(List<WatchParty> watchParties, long totalCount, boolean hasNext) {
            this(watchParties, totalCount, hasNext, Map.of());
        }

        public SearchResult(List<WatchParty> watchParties, long totalCount, boolean hasNext,
                            Map<UUID, Long> participantCounts) {
            this.watchParties = watchParties;
            this.totalCount = totalCount;
            this.hasNext = hasNext;
            this.participantCounts = participantCounts;
        }
    }

    public SearchResult search(WatchPartySearch request) {
        if (request.getSort() == WatchPartySearch.Sort.PARTICIPANT_COUNT) {
            return searchByParticipantCount(request);
        }
        if (request.getContentIdEqual() != null) {
            return searchForContent(request);
        }

        Map<String, Object> params = new HashMap<>();
        String where = buildFilter(request, params);

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
        String jpql = "select w from WatchParty w join fetch w.host" + listWhere
                + " order by w.scheduledAt " + direction + ", w.id " + direction;

        TypedQuery<WatchParty> query = em.createQuery(jpql, WatchParty.class);
        params.forEach(query::setParameter);

        List<WatchParty> fetched = query.setMaxResults(request.getLimit() + 1).getResultList();
        boolean hasNext = fetched.size() > request.getLimit();
        List<WatchParty> result = hasNext ? fetched.subList(0, request.getLimit()) : fetched;

        return new SearchResult(result, totalCount, hasNext);
    }

    private SearchResult searchForContent(WatchPartySearch request) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(buildFilter(request, params));

        TypedQuery<Long> countQuery = em.createQuery(
                "select count(w) from WatchParty w" + where, Long.class);
        params.forEach(countQuery::setParameter);
        long totalCount = countQuery.getSingleResult();

        if (request.getCursorScheduledAt() != null
                && request.getCursorId() != null
                && request.getCursorStatus() != null) {
            if (request.getCursorStatus() == WatchPartyStatus.LIVE) {
                where.append(" and (w.status = :scheduledStatus or (w.status = :liveStatus")
                        .append(" and (w.scheduledAt > :cursorScheduledAt")
                        .append(" or (w.scheduledAt = :cursorScheduledAt and w.id < :cursorId))))");
                params.put("scheduledStatus", WatchPartyStatus.SCHEDULED);
                params.put("liveStatus", WatchPartyStatus.LIVE);
            } else {
                where.append(" and w.status = :scheduledStatus")
                        .append(" and (w.scheduledAt > :cursorScheduledAt")
                        .append(" or (w.scheduledAt = :cursorScheduledAt and w.id < :cursorId))");
                params.put("scheduledStatus", WatchPartyStatus.SCHEDULED);
            }
            params.put("cursorScheduledAt", request.getCursorScheduledAt());
            params.put("cursorId", request.getCursorId());
        }

        String jpql = "select w from WatchParty w join fetch w.host" + where
                + " order by case when w.status = :liveOrderStatus then 0 else 1 end asc,"
                + " w.scheduledAt asc, w.id desc";
        params.put("liveOrderStatus", WatchPartyStatus.LIVE);

        TypedQuery<WatchParty> query = em.createQuery(jpql, WatchParty.class);
        params.forEach(query::setParameter);
        List<WatchParty> fetched = query.setMaxResults(request.getLimit() + 1).getResultList();
        boolean hasNext = fetched.size() > request.getLimit();
        List<WatchParty> result = hasNext ? fetched.subList(0, request.getLimit()) : fetched;

        return new SearchResult(result, totalCount, hasNext);
    }

    // 인기순: ① (id, 참가자 수) 순위표만 뽑고 → ② 그 id들로 파티 상세를 가져와 ①의 순서대로 재정렬
    private SearchResult searchByParticipantCount(WatchPartySearch request) {
        Map<String, Object> params = new HashMap<>();
        String where = buildFilter(request, params);

        TypedQuery<Long> countQuery = em.createQuery(
                "select count(w) from WatchParty w" + where, Long.class);
        params.forEach(countQuery::setParameter);
        long totalCount = countQuery.getSingleResult();

        // ① 순위표: 파티별로 JOINED를 세서 정렬
        String compare = request.isAscending() ? ">" : "<";
        String direction = request.isAscending() ? "asc" : "desc";

        // 커서는 "묶어서 센 값"에 거는 조건이라 where가 아니라 having에 쓴다
        StringBuilder having = new StringBuilder();
        if (request.getCursorParticipantCount() != null && request.getCursorId() != null) {
            having.append(" having count(p) ").append(compare).append(" :cursorCount")
                    .append(" or (count(p) = :cursorCount and w.id ").append(compare).append(" :cursorId)");
            params.put("cursorCount", request.getCursorParticipantCount());
            params.put("cursorId", request.getCursorId());
        }

        // JOINED 조건을 where가 아니라 on에 둬야 참가자 0명 파티도 결과에 남는다
        String rankJpql = "select w.id, count(p) from WatchParty w"
                + " left join WatchPartyParticipant p"
                + " on p.watchParty = w and p.status = :joinedStatus"
                + where
                + " group by w.id"
                + having
                + " order by count(p) " + direction + ", w.id " + direction;
        params.put("joinedStatus", ParticipantStatus.JOINED);

        TypedQuery<Object[]> rankQuery = em.createQuery(rankJpql, Object[].class);
        params.forEach(rankQuery::setParameter);
        List<Object[]> ranked = rankQuery.setMaxResults(request.getLimit() + 1).getResultList();

        boolean hasNext = ranked.size() > request.getLimit();
        List<Object[]> page = hasNext ? ranked.subList(0, request.getLimit()) : ranked;
        if (page.isEmpty()) {
            return new SearchResult(List.of(), totalCount, false);
        }

        // LinkedHashMap: 넣은 순서(= ① 순위)를 기억한다
        Map<UUID, Long> participantCounts = new LinkedHashMap<>();
        for (Object[] row : page) {
            participantCounts.put((UUID) row[0], (Long) row[1]);
        }

        // ② 상세: in 절은 순서를 보장하지 않으므로 id로 찾아 ① 순서대로 다시 담는다
        List<WatchParty> fetched = em.createQuery(
                        "select w from WatchParty w join fetch w.host where w.id in :ids", WatchParty.class)
                .setParameter("ids", new ArrayList<>(participantCounts.keySet()))
                .getResultList();

        Map<UUID, WatchParty> byId = new HashMap<>();
        for (WatchParty watchParty : fetched) {
            byId.put(watchParty.getId(), watchParty);
        }

        List<WatchParty> ordered = new ArrayList<>();
        for (UUID id : participantCounts.keySet()) {
            WatchParty watchParty = byId.get(id);
            if (watchParty != null) {   // ①과 ② 사이에 삭제된 파티는 건너뜀
                ordered.add(watchParty);
            }
        }

        return new SearchResult(ordered, totalCount, hasNext, participantCounts);
    }

    // 세 경로(기본·콘텐츠·인기순)가 공통으로 쓰는 필터. 커서 조건은 경로마다 달라서 여기 넣지 않는다
    private String buildFilter(WatchPartySearch request, Map<String, Object> params) {
        StringBuilder where = new StringBuilder(" where 1=1");

        // 콘텐츠 상세 '더보기': 해당 콘텐츠 + 시작 1시간 이내까지만
        if (request.getContentIdEqual() != null) {
            where.append(" and w.contentId = :contentId")
                    .append(" and w.scheduledAt >= :contentScheduledAtFrom");
            params.put("contentId", request.getContentIdEqual());
            params.put("contentScheduledAtFrom", request.getContentScheduledAtFrom());
        }

        if (request.getStatusEqual() != null) {
            where.append(" and w.status = :status");
            params.put("status", request.getStatusEqual());
        }

        if (request.getKeywordLike() != null) {
            // 콘텐츠는 연관관계 없이 contentId(UUID)만 있으므로 join 대신 서브쿼리 (where 안에서 끝나 세 경로 공통 적용)
            where.append(" and (w.title like :keyword escape '!'")
                    .append(" or w.contentId in (select c.id from Content c where c.title like :keyword escape '!'))");
            params.put("keyword", "%" + escapeLike(request.getKeywordLike()) + "%");
        }

        // 종료 파티 제외: 콘텐츠 검색은 항상, 인기순은 statusEqual이 없을 때
        // (종료 파티는 JOINED 행이 정리되지 않아 인기순 상위에 계속 남기 때문)
        boolean activeOnly = request.getContentIdEqual() != null
                || (request.getSort() == WatchPartySearch.Sort.PARTICIPANT_COUNT
                && request.getStatusEqual() == null);
        if (activeOnly) {
            where.append(" and w.status in (:activeStatuses)");
            params.put("activeStatuses", ACTIVE_STATUSES);
        }

        return where.toString();
    }

    // 검색어 속 LIKE 특수문자(%, _)를 글자 그대로 찾도록 '!'로 탈출. 탈출 문자 자신(!)을 먼저 바꿔야 한다
    private static String escapeLike(String keyword) {
        return keyword.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
