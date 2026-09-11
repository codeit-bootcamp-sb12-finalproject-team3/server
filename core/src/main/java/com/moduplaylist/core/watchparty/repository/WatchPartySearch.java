package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class WatchPartySearch {

    private WatchPartyStatus statusEqual;
    private UUID contentIdEqual;

    private Instant cursorScheduledAt;
    private UUID cursorId;

    @Builder.Default
    private boolean ascending = true;

    private int limit;
}