package com.moduplaylist.api.dm.dto;

import com.moduplaylist.core.dm.entity.DirectMessage;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DirectMessageResponse {

    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String content;
    private Instant createdAt;
    private Instant readAt;

    public static DirectMessageResponse from(DirectMessage message) {
        return new DirectMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getContent(),
                message.getCreatedAt(),
                message.getReadAt()
        );
    }
}
