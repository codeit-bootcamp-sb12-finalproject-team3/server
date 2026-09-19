package com.moduplaylist.realtime.dm.websocket;

import com.moduplaylist.realtime.dm.publisher.DmSendRequestedPublisher;
import java.security.Principal;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class DmMessageController {

    private final DmSendRequestedPublisher publisher;

    public DmMessageController(DmSendRequestedPublisher publisher) {
        this.publisher = publisher;
    }

    @MessageMapping("/dm/messages")
    public void sendMessage(DmSendRequest request, Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());

        publisher.publish(
                request.getConversationId(),
                senderId,
                request.getContent()
        );
    }
}
