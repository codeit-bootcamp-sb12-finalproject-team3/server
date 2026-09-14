package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.entity.WatchPartyPlaybackStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WatchPartyPlaybackState {

    private WatchPartyPlaybackStatus status;
    private Long startedAt;            // epoch millis
    private Long accumulatedPauseMs;
    private Integer startEpisode;      // nullable (영화/스포츠는 null)
    private Integer endEpisode;        // nullable
    private UUID hostId;
    private Long updatedAt;            // epoch millis
}