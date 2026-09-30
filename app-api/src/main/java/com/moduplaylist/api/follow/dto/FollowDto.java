package com.moduplaylist.api.follow.dto;

import com.moduplaylist.core.follow.entity.Follow;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FollowDto {
    private UUID id;
    private UUID followeeId;
    private UUID followerId;

    public static FollowDto from(Follow follow) {
        return new FollowDto(
                follow.getId(),
                follow.getFollowee().getId(),
                follow.getFollower().getId()
        );
    }
}
