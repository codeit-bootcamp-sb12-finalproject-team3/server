package com.moduplaylist.core.follow.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class FollowAlreadyExistsException extends BaseException  {
    // 서비스에서 중복 발견한 경우
    public FollowAlreadyExistsException(UUID followerId, UUID followeeId)
    {
        this(followerId, followeeId, null);
    }

    // 동시 요청으로 인한 DB UNIQUE 제약
    public FollowAlreadyExistsException(UUID followerId, UUID followeeId, Throwable cause) {
        super(ErrorCode.FOLLOW_ALREADY_EXISTS, cause);
        addDetail("followerId", followerId);
        addDetail("followeeId", followeeId);
    }
}
