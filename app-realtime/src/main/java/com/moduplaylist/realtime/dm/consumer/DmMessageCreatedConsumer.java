package com.moduplaylist.realtime.dm.consumer;

import com.moduplaylist.realtime.kafka.KafkaTopics;
import com.moduplaylist.realtime.kafka.event.DmMessageCreatedKafkaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DmMessageCreatedConsumer {

    @KafkaListener(
            topics = KafkaTopics.DM_MESSAGE_CREATED,
            groupId = "${realtime.kafka.dm-message-consumer-group}"
    )
    public void consume(DmMessageCreatedKafkaEvent event) {
        log.debug(
                "Consumed DM message-created event: messageId={}, conversationId={}, "
                        + "senderId={}, receiverId={}",
                event.messageId(),
                event.conversationId(),
                event.senderId(),
                event.receiverId()
        );
    }
}
