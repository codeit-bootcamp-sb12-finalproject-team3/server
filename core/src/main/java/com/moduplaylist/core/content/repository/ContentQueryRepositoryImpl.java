package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ContentQueryRepositoryImpl implements ContentQueryRepository {

    private static final SearchResult EMPTY_RESULT =
            new SearchResult(List.of(), 0L, false, null);

    private final EntityManager entityManager;

    @Override
    public SearchResult search(ContentSearch request) {
        return search(request, request.getIdAfter() == null);
    }

    @Override
    public SearchResult searchWithoutTotalCount(ContentSearch request) {
        return search(request, false);
    }

    private SearchResult search(ContentSearch request, boolean calculateTotalCount) {
        if (request.hasContentIdFilter() && request.getMatchedContentIds().isEmpty()) {
            return calculateTotalCount
                    ? EMPTY_RESULT
                    : new SearchResult(List.of(), null, false, null);
        }
        return request.isLikedContentsSearch()
                ? searchLikedContents(request, calculateTotalCount)
                : searchContents(request, calculateTotalCount);
    }

    @Override
    public SearchResult searchNewContents(NewContentSearch request) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("excludedType", ContentType.TV_SERIES);
        parameters.put("createdAtFrom", request.getCreatedAtFrom());
        StringBuilder filter = new StringBuilder(
                " where content.hidden = false"
                        + " and content.type <> :excludedType"
                        + " and content.createdAt >= :createdAtFrom");
        Long totalCount = request.getIdAfter() == null
                ? countContents(filter, parameters)
                : null;

        if (request.getIdAfter() != null) {
            filter.append(" and (content.createdAt < :cursorCreatedAt")
                    .append(" or (content.createdAt = :cursorCreatedAt")
                    .append(" and content.id < :idAfter))");
            parameters.put("cursorCreatedAt", request.getCursorCreatedAt());
            parameters.put("idAfter", request.getIdAfter());
        }

        TypedQuery<Content> query = entityManager.createQuery(
                "select content from Content content"
                        + filter
                        + " order by content.createdAt desc, content.id desc",
                Content.class);
        setParameters(query, parameters);

        List<Content> fetched = query
                .setMaxResults(request.getLimit() + 1)
                .getResultList();
        boolean hasNext = fetched.size() > request.getLimit();
        return new SearchResult(
                limit(fetched, request.getLimit(), hasNext),
                totalCount,
                hasNext,
                null
        );
    }

    private SearchResult searchContents(
            ContentSearch request,
            boolean calculateTotalCount) {
        Map<String, Object> parameters = new HashMap<>();
        StringBuilder filter = createContentFilter(request, parameters, "content");
        Long totalCount = calculateTotalCount
                ? countContents(filter, parameters)
                : null;

        appendContentCursor(filter, parameters, request);
        String orderBy = request.getSort() == ContentSearch.Sort.LATEST
                ? " order by content.createdAt desc, content.id desc"
                : " order by content.averageRating desc,"
                        + " content.reviewCount desc, content.id desc";

        TypedQuery<Content> query = entityManager.createQuery(
                "select content from Content content" + filter + orderBy,
                Content.class);
        setParameters(query, parameters);

        List<Content> fetched = query
                .setMaxResults(request.getLimit() + 1)
                .getResultList();
        boolean hasNext = fetched.size() > request.getLimit();
        List<Content> contents = limit(fetched, request.getLimit(), hasNext);
        return new SearchResult(contents, totalCount, hasNext, null);
    }

    private SearchResult searchLikedContents(
            ContentSearch request,
            boolean calculateTotalCount) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("likedByUserId", request.getLikedByUserId());

        StringBuilder filter = createContentFilter(request, parameters, "content");
        filter.append(" and contentLike.user.id = :likedByUserId");
        Long totalCount = calculateTotalCount
                ? countLikedContents(filter, parameters)
                : null;

        appendLikedCursor(filter, parameters, request);
        TypedQuery<Object[]> query = entityManager.createQuery(
                "select content, contentLike.createdAt"
                        + " from ContentLike contentLike"
                        + " join contentLike.content content"
                        + filter
                        + " order by contentLike.createdAt desc, content.id asc",
                Object[].class);
        setParameters(query, parameters);

        List<Object[]> fetched = query
                .setMaxResults(request.getLimit() + 1)
                .getResultList();
        boolean hasNext = fetched.size() > request.getLimit();
        int resultSize = hasNext ? request.getLimit() : fetched.size();
        List<Content> contents = new ArrayList<>(resultSize);
        for (int index = 0; index < resultSize; index++) {
            contents.add((Content) fetched.get(index)[0]);
        }

        Instant nextCursorLikedAt = hasNext
                ? (Instant) fetched.get(resultSize - 1)[1]
                : null;
        return new SearchResult(contents, totalCount, hasNext, nextCursorLikedAt);
    }

    private StringBuilder createContentFilter(
            ContentSearch request,
            Map<String, Object> parameters,
            String contentAlias) {
        StringBuilder filter = new StringBuilder(" where ")
                .append(contentAlias)
                .append(".hidden = false and ")
                .append(contentAlias);
        if (request.getType() == null) {
            filter.append(".type <> :excludedType");
            parameters.put("excludedType", ContentType.TV_SERIES);
        } else {
            filter.append(".type = :contentType");
            parameters.put("contentType", request.getType());
        }

        if (request.getSportType() != null && !request.getSportType().isBlank()) {
            filter.append(" and ")
                    .append(contentAlias)
                    .append(".type = :sportContentType")
                    .append(" and exists (select sportEvent.contentId")
                    .append(" from SportEvent sportEvent")
                    .append(" join sportEvent.sportType sportType")
                    .append(" where sportEvent.content = ")
                    .append(contentAlias)
                    .append(" and sportType.code = :sportType)");
            parameters.put("sportContentType", ContentType.SPORT);
            parameters.put("sportType", request.getSportType());
        }
        if (request.getGenreId() != null) {
            filter.append(" and exists (select contentGenre.id")
                    .append(" from ContentGenre contentGenre")
                    .append(" where contentGenre.content = ")
                    .append(contentAlias)
                    .append(" and contentGenre.genre.id = :genreId)");
            parameters.put("genreId", request.getGenreId());
        }
        if (request.hasContentIdFilter()) {
            filter.append(" and ")
                    .append(contentAlias)
                    .append(".id in :matchedContentIds");
            parameters.put("matchedContentIds", request.getMatchedContentIds());
        }
        return filter;
    }

    private long countContents(
            StringBuilder filter,
            Map<String, Object> parameters) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(content) from Content content" + filter,
                Long.class);
        setParameters(query, parameters);
        return query.getSingleResult();
    }

    private long countLikedContents(
            StringBuilder filter,
            Map<String, Object> parameters) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(contentLike)"
                        + " from ContentLike contentLike"
                        + " join contentLike.content content"
                        + filter,
                Long.class);
        setParameters(query, parameters);
        return query.getSingleResult();
    }

    private void appendContentCursor(
            StringBuilder filter,
            Map<String, Object> parameters,
            ContentSearch request) {
        if (request.getIdAfter() == null) {
            return;
        }
        parameters.put("idAfter", request.getIdAfter());

        if (request.getSort() == ContentSearch.Sort.LATEST) {
            filter.append(" and (content.createdAt < :cursorCreatedAt")
                    .append(" or (content.createdAt = :cursorCreatedAt")
                    .append(" and content.id < :idAfter))");
            parameters.put("cursorCreatedAt", request.getCursorCreatedAt());
            return;
        }

        filter.append(" and (content.averageRating < :cursorRating")
                .append(" or (content.averageRating = :cursorRating")
                .append(" and content.reviewCount < :cursorReviewCount)")
                .append(" or (content.averageRating = :cursorRating")
                .append(" and content.reviewCount = :cursorReviewCount")
                .append(" and content.id < :idAfter))");
        parameters.put("cursorRating", request.getCursorRating());
        parameters.put("cursorReviewCount", request.getCursorReviewCount());
    }

    private void appendLikedCursor(
            StringBuilder filter,
            Map<String, Object> parameters,
            ContentSearch request) {
        if (request.getIdAfter() == null) {
            return;
        }
        filter.append(" and (contentLike.createdAt < :cursorLikedAt")
                .append(" or (contentLike.createdAt = :cursorLikedAt")
                .append(" and content.id > :idAfter))");
        parameters.put("cursorLikedAt", request.getCursorLikedAt());
        parameters.put("idAfter", request.getIdAfter());
    }

    private <T> void setParameters(
            TypedQuery<T> query,
            Map<String, Object> parameters) {
        parameters.forEach(query::setParameter);
    }

    private List<Content> limit(
            List<Content> fetched,
            int limit,
            boolean hasNext) {
        return hasNext ? fetched.subList(0, limit) : fetched;
    }
}
