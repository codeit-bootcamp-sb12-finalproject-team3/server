package com.moduplaylist.realtime.watchparty.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WatchPartyParticipantChangedMessage {

    private UUID userId;
    private String status; // JOINED | LEFT | KICKED (core enum 의존 불가 → String)
}