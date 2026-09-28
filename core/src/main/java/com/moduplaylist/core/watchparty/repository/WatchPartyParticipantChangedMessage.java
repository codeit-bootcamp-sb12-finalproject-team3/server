package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WatchPartyParticipantChangedMessage {

    private UUID userId;
    private ParticipantStatus status; // JOINED | LEFT | KICKED
}