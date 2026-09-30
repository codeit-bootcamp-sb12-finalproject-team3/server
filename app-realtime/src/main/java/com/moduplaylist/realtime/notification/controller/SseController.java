package com.moduplaylist.realtime.notification.controller;

import com.moduplaylist.realtime.global.security.RealtimePrincipal;
import com.moduplaylist.realtime.notification.sse.NotificationSseService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
public class SseController {

    private final NotificationSseService notificationSseService;

    public SseController(NotificationSseService notificationSseService) {
        this.notificationSseService = notificationSseService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal RealtimePrincipal principal) {
        return notificationSseService.connect(principal.userId());
    }

}
