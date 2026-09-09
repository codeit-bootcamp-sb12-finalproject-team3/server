package com.moduplaylist.core.recommendation.repository;

import com.moduplaylist.core.recommendation.entity.UserContentTagPreference;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserContentTagPreferenceRepository
        extends JpaRepository<UserContentTagPreference, UUID> {

    Optional<UserContentTagPreference> findByUser_IdAndTag_Id(UUID userId, UUID tagId);

    List<UserContentTagPreference> findAllByUser_Id(UUID userId);
}
