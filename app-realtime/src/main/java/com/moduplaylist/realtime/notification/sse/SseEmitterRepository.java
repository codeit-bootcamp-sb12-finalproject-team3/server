package com.moduplaylist.realtime.notification.sse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterRepository {

    private final ConcurrentMap<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByUser =
            new ConcurrentHashMap<>();

    public void save(UUID userId, SseEmitter emitter) {
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>())
                .add(emitter);
    }

    public List<SseEmitter> findAllByUserId(UUID userId) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        return emitters == null ? List.of() : List.copyOf(emitters);
    }

    public void delete(UUID userId, SseEmitter emitter) {
        emittersByUser.computeIfPresent(userId, (ignored, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
