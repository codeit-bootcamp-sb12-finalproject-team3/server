package com.moduplaylist.core.review.repository;

import com.moduplaylist.core.review.entity.Review;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReviewQueryRepositoryImpl implements ReviewQueryRepository {

    private final EntityManager entityManager;

    @Override
    public SearchResult search(SearchCondition condition) {
        Map<String, Object> parameters = new HashMap<>();
        StringBuilder filter = createFilter(condition, parameters);
        long totalCount = countReviews(filter, parameters);

        appendCursor(filter, parameters, condition);
        TypedQuery<Review> query = entityManager.createQuery("""
                        select review
                        from Review review
                        join fetch review.user
                        join fetch review.content
                        """ + filter + createOrderBy(condition),
                Review.class);
        setParameters(query, parameters);

        List<Review> fetched = query
                .setMaxResults(condition.getLimit() + 1)
                .getResultList();
        boolean hasNext = fetched.size() > condition.getLimit();
        List<Review> reviews = hasNext
                ? fetched.subList(0, condition.getLimit())
                : fetched;

        return new SearchResult(reviews, totalCount, hasNext);
    }

    private StringBuilder createFilter(
            SearchCondition condition,
            Map<String, Object> parameters) {
        if (condition.getContentId() == null) {
            return new StringBuilder();
        }
        parameters.put("contentId", condition.getContentId());
        return new StringBuilder(" where review.content.id = :contentId");
    }

    private long countReviews(
            StringBuilder filter,
            Map<String, Object> parameters) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(review) from Review review" + filter,
                Long.class);
        setParameters(query, parameters);
        return query.getSingleResult();
    }

    private void appendCursor(
            StringBuilder filter,
            Map<String, Object> parameters,
            SearchCondition condition) {
        if (condition.getIdAfter() == null) {
            return;
        }

        String sortProperty;
        Object cursorValue;
        if (condition.getSort() == Sort.CREATED_AT) {
            sortProperty = "createdAt";
            cursorValue = condition.getCursorCreatedAt();
        } else {
            sortProperty = "rating";
            cursorValue = condition.getCursorRating();
        }
        String operator = condition.getDirection() == Direction.ASCENDING ? ">" : "<";
        filter.append(filter.isEmpty() ? " where " : " and ")
                .append("(review.").append(sortProperty).append(" ").append(operator)
                .append(" :cursorValue or (review.").append(sortProperty)
                .append(" = :cursorValue and review.id ").append(operator)
                .append(" :idAfter))");
        parameters.put("cursorValue", cursorValue);
        parameters.put("idAfter", condition.getIdAfter());
    }

    private String createOrderBy(SearchCondition condition) {
        String sortProperty = condition.getSort() == Sort.CREATED_AT
                ? "createdAt"
                : "rating";
        String direction = condition.getDirection() == Direction.ASCENDING
                ? " asc"
                : " desc";
        return " order by review." + sortProperty + direction
                + ", review.id" + direction;
    }

    private <T> void setParameters(
            TypedQuery<T> query,
            Map<String, Object> parameters) {
        parameters.forEach(query::setParameter);
    }
}
