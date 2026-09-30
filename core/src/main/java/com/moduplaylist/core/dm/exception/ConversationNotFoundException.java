package com.moduplaylist.core.dm.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class ConversationNotFoundException extends BaseException {
    public ConversationNotFoundException(UUID conversationId) {
        super(ErrorCode.CONVERSATION_NOT_FOUND);
        addDetail("conversationId", conversationId);
    }
}
