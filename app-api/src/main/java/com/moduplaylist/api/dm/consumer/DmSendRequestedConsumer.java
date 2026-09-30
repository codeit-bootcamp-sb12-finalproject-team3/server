package com.moduplaylist.api.dm.consumer;

import com.moduplaylist.api.dm.service.DirectMessageService;
import com.moduplaylist.core.dm.exception.ConversationAccessDeniedException;
import com.moduplaylist.core.dm.exception.ConversationNotFoundException;
import com.moduplaylist.core.dm.exception.InvalidDirectMessageContentException;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.DmSendRequestedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DmSendRequestedConsumer {

    private final DirectMessageService directMessageService;

    @KafkaListener(
            topics = KafkaTopics.DM_SEND_REQUESTED,
            groupId = "dm-message-persistence"
    )
    public void consume(DmSendRequestedKafkaEvent event) {
        try {
            directMessageService.createMessage(
                    event.conversationId(),
                    event.senderId(),
                    event.content()
            );
        } catch (
                ConversationNotFoundException
                | ConversationAccessDeniedException
                | InvalidDirectMessageContentException exception
        ) {
            log.warn(
                    "Ignoring permanent DM send failure: conversationId={}, senderId={}, errorCode={}",
                    event.conversationId(),
                    event.senderId(),
                    exception.getErrorCode()
            );
        }
    }
}
