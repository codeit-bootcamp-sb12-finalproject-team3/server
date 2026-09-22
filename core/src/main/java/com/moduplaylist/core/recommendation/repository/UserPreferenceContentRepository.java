package com.moduplaylist.core.recommendation.repository;

import com.moduplaylist.core.recommendation.entity.UserPreferenceContent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPreferenceContentRepository
        extends JpaRepository<UserPreferenceContent, UUID> {

    List<UserPreferenceContent> findAllByUser_Id(UUID userId);

    @Query("select preference.content.id from UserPreferenceContent preference "
            + "where preference.user.id = :userId")
    List<UUID> findContentIdsByUserId(@Param("userId") UUID userId);

    boolean existsByUser_Id(UUID userId);
}
