package com.moduplaylist.realtime.dm.consumer;

import com.moduplaylist.realtime.dm.redis.DmRedisMessage;
import com.moduplaylist.realtime.dm.redis.DmRedisPublisher;
import com.moduplaylist.realtime.kafka.KafkaTopics;
import com.moduplaylist.realtime.kafka.event.DmMessageCreatedKafkaEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DmMessageCreatedConsumer {

    private final DmRedisPublisher dmRedisPublisher;

    public DmMessageCreatedConsumer(DmRedisPublisher dmRedisPublisher) {
        this.dmRedisPublisher = dmRedisPublisher;
    }

    @KafkaListener(
            topics = KafkaTopics.DM_MESSAGE_CREATED,
            groupId = "${realtime.kafka.dm-message-consumer-group}"
    )
    public void consume(DmMessageCreatedKafkaEvent event) {
        DmRedisMessage message = new DmRedisMessage(
                event.messageId(),
                event.conversationId(),
                event.senderId(),
                event.receiverId(),
                event.content(),
                event.createdAt()
        );

        dmRedisPublisher.publish(message);
    }
}
