package com.moduplaylist.api.notification.controller;

import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> delete (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID notificationId
    ) {
        UUID receiverId = userDetails.getUserId();
        notificationService.delete(receiverId, notificationId);
        return ResponseEntity.noContent().build();
    }
}
