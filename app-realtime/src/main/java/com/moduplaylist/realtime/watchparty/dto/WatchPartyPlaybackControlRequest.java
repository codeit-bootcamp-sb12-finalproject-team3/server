package com.moduplaylist.realtime.watchparty.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WatchPartyPlaybackControlRequest {

    private WatchPartyPlaybackAction action;
    private Long targetElapsedMs;  // SEEK일 때만 사용, PLAY/PAUSE는 null
}