package com.moduplaylist.realtime.dm.consumer;

import com.moduplaylist.realtime.dm.websocket.DmMessageCreatedPayload;
import com.moduplaylist.realtime.dm.websocket.DmRealtimeDeliveryService;
import com.moduplaylist.realtime.kafka.KafkaTopics;
import com.moduplaylist.realtime.kafka.event.DmMessageCreatedKafkaEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DmMessageCreatedConsumer {

    private final DmRealtimeDeliveryService deliveryService;

    public DmMessageCreatedConsumer(DmRealtimeDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @KafkaListener(
            topics = KafkaTopics.DM_MESSAGE_CREATED,
            groupId = "${realtime.kafka.dm-message-consumer-group}"
    )
    public void consume(DmMessageCreatedKafkaEvent event) {
        DmMessageCreatedPayload payload = new DmMessageCreatedPayload(
                event.messageId(),
                event.conversationId(),
                event.senderId(),
                event.receiverId(),
                event.content(),
                event.createdAt()
        );

        deliveryService.deliver(
                event.senderId(),
                event.receiverId(),
                payload
        );
    }
}
