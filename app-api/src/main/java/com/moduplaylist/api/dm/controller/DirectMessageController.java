package com.moduplaylist.api.dm.controller;

import com.moduplaylist.api.dm.dto.ConversationCreateRequest;
import com.moduplaylist.api.dm.dto.ConversationResponse;
import com.moduplaylist.api.dm.dto.ConversationSearchRequest;
import com.moduplaylist.api.dm.dto.DirectMessageReadRequest;
import com.moduplaylist.api.dm.dto.DirectMessageResponse;
import com.moduplaylist.api.dm.dto.DirectMessageSearchRequest;
import com.moduplaylist.api.dm.service.DirectMessageService;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dm/conversations")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;

    @PostMapping
    public ResponseEntity<ConversationResponse> createOrGetConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ConversationCreateRequest request
    ) {
        return ResponseEntity.ok(directMessageService.createOrGetConversation(
                userDetails.getUserId(),
                request.getPeerId()
        ));
    }

    @GetMapping
    public ResponseEntity<CursorPageResponse<ConversationResponse>> getConversations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute ConversationSearchRequest request
    ) {
        return ResponseEntity.ok(
                directMessageService.getConversations(userDetails.getUserId(), request)
        );
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<CursorPageResponse<DirectMessageResponse>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID conversationId,
            @Valid @ModelAttribute DirectMessageSearchRequest request
    ) {
        return ResponseEntity.ok(directMessageService.getMessages(
                userDetails.getUserId(),
                conversationId,
                request
        ));
    }

    @PatchMapping("/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID conversationId,
            @Valid @RequestBody DirectMessageReadRequest request
    ) {
        directMessageService.markAsRead(
                userDetails.getUserId(),
                conversationId,
                request.getLastReadMessageId()
        );
        return ResponseEntity.noContent().build();
    }
}
