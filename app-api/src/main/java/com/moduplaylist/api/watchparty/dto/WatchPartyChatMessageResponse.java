package com.moduplaylist.api.watchparty.dto;

import com.moduplaylist.core.watchparty.repository.WatchPartyChatMessage;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WatchPartyChatMessageResponse {

    private UUID senderId;
    private String content;
    private Instant sentAt;

    public static WatchPartyChatMessageResponse from(WatchPartyChatMessage message) {
        Long sentAtMillis = message.getSentAt();

        return WatchPartyChatMessageResponse.builder()
                .senderId(message.getSenderId())
                .content(message.getContent())
                .sentAt(sentAtMillis == null ? null : Instant.ofEpochMilli(sentAtMillis))
                .build();
    }
}