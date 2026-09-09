package com.moduplaylist.core.follow.repository;

import com.moduplaylist.core.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    boolean existsByFollower_IdAndFollowee_Id(UUID followerId, UUID followeeId);

    Optional<Follow> findByFollower_IdAndFollowee_Id(UUID followerId, UUID followeeId);

    long countByFollowee_Id(UUID followeeId);
}
