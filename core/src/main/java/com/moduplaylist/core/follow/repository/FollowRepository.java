package com.moduplaylist.core.follow.repository;

import com.moduplaylist.core.follow.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    boolean existsByFollower_IdAndFollowee_Id(UUID followerId, UUID followeeId);
}
