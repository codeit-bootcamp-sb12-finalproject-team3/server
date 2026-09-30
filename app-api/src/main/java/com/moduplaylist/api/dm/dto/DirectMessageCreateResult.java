package com.moduplaylist.api.dm.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DirectMessageCreateResult {

    private UUID messageId;
    private UUID conversationId;
    private UUID senderId;
    private UUID receiverId;
    private String content;
    private Instant createdAt;
}
