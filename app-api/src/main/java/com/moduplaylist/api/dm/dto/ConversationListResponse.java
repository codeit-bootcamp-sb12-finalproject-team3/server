package com.moduplaylist.api.dm.dto;

import com.moduplaylist.api.user.dto.UserSummary;
import com.moduplaylist.core.dm.entity.Conversation;
import com.moduplaylist.core.dm.entity.DirectMessage;
import com.moduplaylist.core.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConversationListResponse {

    private UUID id;
    private UserSummary peer;
    private DirectMessageResponse latestMessage;
    private boolean hasUnread;
    private Instant createdAt;
    private Instant updatedAt;

    public static ConversationListResponse from(
            Conversation conversation,
            User peer,
            DirectMessage latestMessage,
            boolean hasUnread
    ) {
        return new ConversationListResponse(
                conversation.getId(),
                UserSummary.from(peer),
                latestMessage == null ? null : DirectMessageResponse.from(latestMessage),
                hasUnread,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
