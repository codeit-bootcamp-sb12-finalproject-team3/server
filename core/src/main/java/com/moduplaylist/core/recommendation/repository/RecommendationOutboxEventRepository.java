package com.moduplaylist.core.recommendation.repository;

import com.moduplaylist.core.recommendation.entity.RecommendationOutboxEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationOutboxEventRepository
        extends JpaRepository<RecommendationOutboxEvent, UUID> {

    Optional<RecommendationOutboxEvent> findByEventId(UUID eventId);
}
