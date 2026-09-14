package com.moduplaylist.core.review.repository;

import com.moduplaylist.core.review.entity.Review;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository
        extends JpaRepository<Review, UUID>, ReviewQueryRepository {

    boolean existsByUserIdAndContentId(UUID userId, UUID contentId);

    @Query("""
            select
                review.user.id as userId,
                review.content.id as contentId
            from Review review
            where review.id = :reviewId
            """)
    Optional<ReviewAccessProjection> findAccessById(
            @Param("reviewId") UUID reviewId);

    @Query("""
            select review
            from Review review
            join fetch review.user
            join fetch review.content
            where review.id = :reviewId
            """)
    Optional<Review> findByIdWithUserAndContent(
            @Param("reviewId") UUID reviewId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select review
            from Review review
            join fetch review.user
            join fetch review.content
            where review.id = :reviewId
            """)
    Optional<Review> findByIdForUpdate(@Param("reviewId") UUID reviewId);

    @Query(value = """
            select
                coalesce(round(avg(review.rating), 2), 0.00) as averageRating,
                count(*) as reviewCount
            from reviews review
            where review.content_id = :contentId
            """, nativeQuery = true)
    ReviewStatisticsProjection calculateStatistics(
            @Param("contentId") UUID contentId);

    interface ReviewAccessProjection {

        UUID getUserId();

        UUID getContentId();
    }

    interface ReviewStatisticsProjection {

        BigDecimal getAverageRating();

        long getReviewCount();
    }
}
