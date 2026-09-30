package com.moduplaylist.realtime.notification.consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


import com.moduplaylist.realtime.kafka.event.NotificationCreatedKafkaEvent;
import com.moduplaylist.realtime.notification.sse.NotificationSsePayload;
import com.moduplaylist.realtime.notification.sse.NotificationSseService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationCreatedConsumerTest {

    @Test
    void delegatesTheConsumedContractEventToSseDelivery() {
        NotificationSseService sseService = mock(NotificationSseService.class);
        NotificationCreatedConsumer consumer = new NotificationCreatedConsumer(sseService);
        NotificationCreatedKafkaEvent event = new NotificationCreatedKafkaEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "title",
                "content",
                "INFO",
                Instant.now()
        );

        consumer.consume(event);

        NotificationSsePayload expectedPayload = new NotificationSsePayload(
                event.notificationId(),
                event.title(),
                event.content(),
                event.level(),
                event.createdAt()
        );
        verify(sseService).send(event.receiverId(), expectedPayload);
    }
}
