package com.moduplaylist.api.notification.controller;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.notification.dto.NotificationDto;
import com.moduplaylist.api.notification.dto.NotificationRequest;
import com.moduplaylist.api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> delete (
//            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID notificationId
    ) {
//        UUID receiverId = userDetails.getUserId();
        UUID receiverId =
                UUID.fromString("01a07a54-7f70-7923-92e6-c3d55f004399");
        notificationService.delete(receiverId, notificationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<CursorPageResponse<NotificationDto>> findNotifications(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute NotificationRequest request
            ) {
        UUID receiverId =
                UUID.fromString("01a07a54-7f70-7923-92e6-c3d55f004399");

        return ResponseEntity.ok(
                notificationService.getNotifications(
//                        userDetails.getUserId(),
                        receiverId,
                        request

                )
        ) ;
    }

}
