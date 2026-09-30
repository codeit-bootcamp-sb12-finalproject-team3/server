package com.moduplaylist.core.dm.repository;

import com.moduplaylist.core.dm.entity.Conversation;
import com.moduplaylist.core.dm.entity.DirectMessage;
import com.moduplaylist.core.user.entity.User;

public interface ConversationListItem {

    Conversation getConversation();

    User getPeer();

    DirectMessage getLatestMessage();

    Boolean getHasUnread();
}
