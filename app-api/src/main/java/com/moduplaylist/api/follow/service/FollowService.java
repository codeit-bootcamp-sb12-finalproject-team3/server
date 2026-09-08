package com.moduplaylist.api.follow.service;

import com.moduplaylist.api.follow.dto.FollowDto;
import com.moduplaylist.api.follow.dto.FollowRequest;

import java.util.UUID;

public interface FollowService {
    FollowDto create(UUID followeeId, UUID followerId);
}
