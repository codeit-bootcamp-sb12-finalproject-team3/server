package com.moduplaylist.api.dm.service;

import com.moduplaylist.api.dm.dto.ConversationResponse;
import com.moduplaylist.api.dm.dto.ConversationSearchRequest;
import com.moduplaylist.api.dm.dto.DirectMessageResponse;
import com.moduplaylist.api.dm.dto.DirectMessageSearchRequest;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import java.util.UUID;

public interface DirectMessageService {

    ConversationResponse createOrGetConversation(UUID userId, UUID peerId);

    CursorPageResponse<ConversationResponse> getConversations(
            UUID userId,
            ConversationSearchRequest request
    );

    CursorPageResponse<DirectMessageResponse> getMessages(
            UUID userId,
            UUID conversationId,
            DirectMessageSearchRequest request
    );

    void markAsRead(UUID userId, UUID conversationId, UUID lastReadMessageId);
}
