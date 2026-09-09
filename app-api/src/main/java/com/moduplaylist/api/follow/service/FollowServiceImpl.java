package com.moduplaylist.api.follow.service;

import com.moduplaylist.api.follow.dto.FollowDto;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.follow.entity.Follow;
import com.moduplaylist.core.follow.exception.FollowAlreadyExistsException;
import com.moduplaylist.core.follow.exception.FollowNotFoundException;
import com.moduplaylist.core.follow.exception.SelfFollowNotAllowedException;
import com.moduplaylist.core.follow.repository.FollowRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService{

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public FollowDto create(UUID followerId, UUID followeeId) {
        if (followerId == null) throw new BaseException(ErrorCode.UNAUTHORIZED);
        if (followeeId == null) throw new BaseException(ErrorCode.INVALID_REQUEST);
        User follower = userRepository.findById(followerId).orElseThrow(
                () -> new BaseException(ErrorCode.UNAUTHORIZED)
        );
        User followee = userRepository.findById(followeeId).orElseThrow(
                () -> {
                    BaseException exception = new BaseException(ErrorCode.USER_NOT_FOUND);
                    exception.addDetail("followeeId", followeeId);
                    return exception;
                }
        );

        // follower followee 같은 경우
        if (followeeId.equals(followerId)) {
            throw new SelfFollowNotAllowedException(followerId);
        }

        // follow 이미 존재하는 경우
        if (followRepository.existsByFollower_IdAndFollowee_Id(followerId, followeeId)) {
            throw new FollowAlreadyExistsException(followerId, followeeId);
        }

        Follow follow = new Follow(follower, followee);
        try {
            Follow savedFollow = followRepository.saveAndFlush(follow);
            return FollowDto.from(savedFollow);
        } catch (DataIntegrityViolationException e) {
            throw new FollowAlreadyExistsException(followerId, followeeId, e);
        }
    }

    @Transactional
    @Override
    public void delete(UUID followerId, UUID followeeId) {
        if (followerId == null) throw new BaseException(ErrorCode.UNAUTHORIZED);
        if (followeeId == null) throw new BaseException(ErrorCode.INVALID_REQUEST);

        Follow follow = followRepository.findByFollower_IdAndFollowee_Id(followerId, followeeId)
                .orElseThrow(() -> new FollowNotFoundException(followerId, followeeId));

        followRepository.delete(follow);

    }

    @Transactional(readOnly = true)
    @Override
    public long getFollowerCount(UUID targetUserId) {
        if (targetUserId == null) throw new BaseException(ErrorCode.INVALID_REQUEST);

        validateTargetUser(targetUserId);

        return followRepository.countByFollowee_Id(targetUserId);
    }

    @Transactional(readOnly = true)
    @Override
    public FollowDto getFollow(UUID followerId, UUID followeeId) {
        if (followerId == null) throw new BaseException(ErrorCode.UNAUTHORIZED);
        if (followeeId == null) throw new BaseException(ErrorCode.INVALID_REQUEST);

        validateTargetUser(followeeId);

        Follow follow = followRepository.findByFollower_IdAndFollowee_Id(followerId, followeeId)
                .orElseThrow(() -> new FollowNotFoundException(followerId, followeeId));

        return FollowDto.from(follow);

    }

    private void validateTargetUser(UUID targetUserId) {
        userRepository.findById(targetUserId)
                .orElseThrow(() -> {
                    BaseException exception =
                            new BaseException(ErrorCode.USER_NOT_FOUND);
                    exception.addDetail("userId", targetUserId);
                    return exception;
                });
    }
}
