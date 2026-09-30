package com.moduplaylist.realtime.notification.sse;


import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificationSseService {

    static final String NOTIFICATION_EVENT_NAME = "notification.created";
    static final String CONNECTED_EVENT_NAME = "connected";

    private final SseEmitterRepository emitterRepository;
    private final long timeoutMillis;

    public NotificationSseService(
            SseEmitterRepository emitterRepository,
            @Value("${realtime.sse.timeout-millis:1800000}") long timeoutMillis
    ) {
        this.emitterRepository = emitterRepository;
        this.timeoutMillis = timeoutMillis;
    }

    public SseEmitter connect(UUID userId) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        Runnable cleanup = () -> emitterRepository.delete(userId, emitter);

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        emitterRepository.save(userId, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name(CONNECTED_EVENT_NAME)
                    .data("connected"));
        } catch (IOException | IllegalStateException exception) {
            cleanup.run();
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    public void send(UUID receiverId, NotificationSsePayload payload) {
        for (SseEmitter emitter : emitterRepository.findAllByUserId(receiverId)) {
            try {
                emitter.send(SseEmitter.event()
                        .id(payload.id().toString())
                        .name(NOTIFICATION_EVENT_NAME)
                        .data(payload));
            } catch (IOException | IllegalStateException exception) {
                emitterRepository.delete(receiverId, emitter);
                emitter.completeWithError(exception);
            }
        }
    }
}
