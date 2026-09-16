package com.moduplaylist.core.dm.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class ConversationAccessDeniedException extends BaseException {
    public ConversationAccessDeniedException(UUID conversationId) {
        super(ErrorCode.CONVERSATION_ACCESS_DENIED);
        addDetail("conversationId", conversationId);
    }
}
