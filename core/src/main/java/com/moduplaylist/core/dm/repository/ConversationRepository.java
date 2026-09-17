package com.moduplaylist.core.dm.repository;

import com.moduplaylist.core.dm.entity.Conversation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
}
