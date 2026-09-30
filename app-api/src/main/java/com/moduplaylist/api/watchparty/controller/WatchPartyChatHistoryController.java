package com.moduplaylist.api.watchparty.controller;

import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.watchparty.dto.WatchPartyChatMessageResponse;
import com.moduplaylist.api.watchparty.service.WatchPartyChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/watch-parties/{partyId}/chat-messages")
@RequiredArgsConstructor
public class WatchPartyChatHistoryController {

    private final WatchPartyChatHistoryService watchPartyChatHistoryService;

    @GetMapping
    public ResponseEntity<List<WatchPartyChatMessageResponse>> getRecentMessages(
            @PathVariable UUID partyId,
            @RequestParam(defaultValue = "50") int limit,  // 말풍선(메시지) 50개. 변경 가능
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                watchPartyChatHistoryService.getRecentMessages(partyId, userDetails.getUserId(), limit));
    }
}