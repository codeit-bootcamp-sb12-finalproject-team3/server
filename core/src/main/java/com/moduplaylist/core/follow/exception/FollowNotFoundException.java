package com.moduplaylist.core.follow.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class FollowNotFoundException extends BaseException {
    public FollowNotFoundException(UUID followerId, UUID followeeId) {
        super(ErrorCode.FOLLOW_NOT_FOUND);
        addDetail("followerId", followerId);
        addDetail("followeeId", followeeId);
    }
}
