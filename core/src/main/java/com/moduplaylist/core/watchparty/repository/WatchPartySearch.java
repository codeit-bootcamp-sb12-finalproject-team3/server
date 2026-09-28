package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class WatchPartySearch {

    // ① 정렬 기준
    public enum Sort {
        SCHEDULED_AT,
        PARTICIPANT_COUNT
    }

    private WatchPartyStatus statusEqual;
    private UUID contentIdEqual;

    // ② 기본값 = 기존 동작(시작 시각순)
    @Builder.Default
    private Sort sort = Sort.SCHEDULED_AT;

    private Instant cursorScheduledAt;
    private UUID cursorId;
    private WatchPartyStatus cursorStatus;
    private Instant contentScheduledAtFrom;

    // ③ 인기순 커서: "참가자 N명, 이 id 다음부터"의 N
    private Long cursorParticipantCount;

    @Builder.Default
    private boolean ascending = true;

    private int limit;
}
