package com.moduplaylist.api.follow.event;

import java.util.UUID;

public record FollowCreatedEvent(
        UUID followerId,
        UUID followeeId
) {

}
