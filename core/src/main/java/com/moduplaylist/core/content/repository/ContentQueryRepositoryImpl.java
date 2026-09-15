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
        if (request.hasContentIdFilter() && request.getMatchedContentIds().isEmpty()) {
            return EMPTY_RESULT;
        }
        return request.isLikedContentsSearch()
                ? searchLikedContents(request)
                : searchContents(request);
    }

    private SearchResult searchContents(ContentSearch request) {
        Map<String, Object> parameters = new HashMap<>();
        StringBuilder filter = createContentFilter(request, parameters, "content");
        long totalCount = countContents(filter, parameters);

        appendContentCursor(filter, parameters, request);
        String orderBy = request.getSort() == ContentSearch.Sort.LATEST
                ? " order by content.createdAt desc, content.id desc"
                : " order by content.averageRating desc, content.id desc";

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

    private SearchResult searchLikedContents(ContentSearch request) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("likedByUserId", request.getLikedByUserId());

        StringBuilder filter = createContentFilter(request, parameters, "content");
        filter.append(" and contentLike.user.id = :likedByUserId");
        long totalCount = countLikedContents(filter, parameters);

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
        return new SearchResult(
                contents,
                totalCount,
                hasNext,
                nextCursorLikedAt);
    }

    private StringBuilder createContentFilter(
            ContentSearch request,
            Map<String, Object> parameters,
            String contentAlias) {
        StringBuilder filter = new StringBuilder(" where ")
                .append(contentAlias)
                .append(".type <> :excludedType");
        parameters.put("excludedType", ContentType.TV_SERIES);

        if (request.getSportType() != null && !request.getSportType().isBlank()) {
            filter.append(" and ")
                    .append(contentAlias)
                    .append(".type = :sportContentType")
                    .append(" and ")
                    .append(contentAlias)
                    .append(".sportType = :sportType");
            parameters.put("sportContentType", ContentType.SPORT);
            parameters.put("sportType", request.getSportType());
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
        if (request.getCursorId() == null) {
            return;
        }
        parameters.put("cursorId", request.getCursorId());

        if (request.getSort() == ContentSearch.Sort.LATEST) {
            filter.append(" and (content.createdAt < :cursorCreatedAt")
                    .append(" or (content.createdAt = :cursorCreatedAt")
                    .append(" and content.id < :cursorId))");
            parameters.put("cursorCreatedAt", request.getCursorCreatedAt());
            return;
        }

        filter.append(" and (content.averageRating < :cursorRating")
                .append(" or (content.averageRating = :cursorRating")
                .append(" and content.id < :cursorId))");
        parameters.put("cursorRating", request.getCursorRating());
    }

    private void appendLikedCursor(
            StringBuilder filter,
            Map<String, Object> parameters,
            ContentSearch request) {
        if (request.getCursorId() == null) {
            return;
        }
        filter.append(" and (contentLike.createdAt < :cursorLikedAt")
                .append(" or (contentLike.createdAt = :cursorLikedAt")
                .append(" and content.id > :cursorId))");
        parameters.put("cursorLikedAt", request.getCursorLikedAt());
        parameters.put("cursorId", request.getCursorId());
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
