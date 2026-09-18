package com.moduplaylist.realtime.dm.websocket;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DmSendRequest {

    private UUID conversationId;
    private String content;
}
