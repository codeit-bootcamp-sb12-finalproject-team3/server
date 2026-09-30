package com.moduplaylist.realtime.dm.websocket;

import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class DmRealtimeDeliveryService {

    private static final String DM_DESTINATION = "/queue/dm";

    private final SimpMessagingTemplate messagingTemplate;

    public DmRealtimeDeliveryService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void deliver(
            UUID senderId,
            UUID receiverId,
            DmMessageCreatedPayload payload
    ) {
        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                DM_DESTINATION,
                payload
        );
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                DM_DESTINATION,
                payload
        );
    }
}
