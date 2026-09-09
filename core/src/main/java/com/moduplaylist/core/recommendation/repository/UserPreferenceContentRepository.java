package com.moduplaylist.core.recommendation.repository;

import com.moduplaylist.core.recommendation.entity.UserPreferenceContent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceContentRepository
        extends JpaRepository<UserPreferenceContent, UUID> {

    List<UserPreferenceContent> findAllByUser_Id(UUID userId);

    boolean existsByUser_Id(UUID userId);
}
