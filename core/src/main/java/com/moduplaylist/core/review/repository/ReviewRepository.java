package com.moduplaylist.core.review.repository;

import com.moduplaylist.core.review.Review;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Review r where r.id = :id")
    Optional<Review> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByUser_IdAndContent_Id(UUID userId, UUID contentId);

    Optional<Review> findByUser_IdAndContent_Id(UUID userId, UUID contentId);
}
