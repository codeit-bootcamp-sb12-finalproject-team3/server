package com.moduplaylist.api.follow.service;

import com.moduplaylist.api.follow.dto.FollowDto;

import java.util.UUID;

public interface FollowService {
    FollowDto create(UUID followerId, UUID followeeId);
    void delete(UUID followerId, UUID followeeId);
    long getFollowerCount(UUID followeeId);
    FollowDto getFollow(UUID followerId, UUID followeeId);
}
