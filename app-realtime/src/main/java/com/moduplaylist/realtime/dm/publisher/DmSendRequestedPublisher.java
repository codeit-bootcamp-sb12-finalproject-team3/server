package com.moduplaylist.realtime.dm.publisher;

import com.moduplaylist.realtime.kafka.KafkaTopics;
import com.moduplaylist.realtime.kafka.event.DmSendRequestedKafkaEvent;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DmSendRequestedPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DmSendRequestedPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(UUID conversationId, UUID senderId, String content) {
        DmSendRequestedKafkaEvent event = new DmSendRequestedKafkaEvent(
                conversationId,
                senderId,
                content
        );

        kafkaTemplate.send(
                KafkaTopics.DM_SEND_REQUESTED,
                conversationId.toString(),
                event
        );
    }
}
