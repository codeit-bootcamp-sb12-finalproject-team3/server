package com.moduplaylist.api.watchparty.controller;

import com.moduplaylist.api.watchparty.service.WatchPartyStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/watch-parties/{partyId}")
@RequiredArgsConstructor
public class WatchPartyStatusController {

    private final WatchPartyStatusService watchPartyStatusService;

    @PatchMapping("/start")
    public ResponseEntity<Void> start(@PathVariable UUID partyId) {
        // TODO Security 구현 후 @AuthenticationPrincipal CustomUserDetails로 변경
        UUID hostId = UUID.fromString("01a07a54-7f70-7e0d-9a80-15abccab6c92");

        watchPartyStatusService.startWatchParty(partyId, hostId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/end")
    public ResponseEntity<Void> end(@PathVariable UUID partyId) {
        // TODO Security 구현 후 @AuthenticationPrincipal CustomUserDetails로 변경
        UUID hostId = UUID.fromString("01a07a54-7f70-7e0d-9a80-15abccab6c92");

        watchPartyStatusService.endWatchParty(partyId, hostId);
        return ResponseEntity.noContent().build();
    }
}