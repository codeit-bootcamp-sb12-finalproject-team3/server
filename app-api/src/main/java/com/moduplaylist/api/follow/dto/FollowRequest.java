package com.moduplaylist.api.follow.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class FollowRequest {
    private UUID followeeId;
}
