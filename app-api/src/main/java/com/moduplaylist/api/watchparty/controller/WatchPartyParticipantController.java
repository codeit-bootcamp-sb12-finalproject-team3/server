package com.moduplaylist.api.watchparty.controller;

import com.moduplaylist.api.watchparty.service.WatchPartyParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/watch-parties/{partyId}/participants")
@RequiredArgsConstructor
public class WatchPartyParticipantController {

    private final WatchPartyParticipantService watchPartyParticipantService;

    @PostMapping
    public ResponseEntity<Void> join(@PathVariable UUID partyId) {
        //TODO Security 구현 후 @AuthenticationPrincipal CustomUserDetails로 변경
        UUID userId = UUID.fromString("01a07a54-7f70-7e0d-9a80-15abccab6c92");

        watchPartyParticipantService.joinWatchParty(partyId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> leave(@PathVariable UUID partyId) {
        //TODO Security 구현 후 @AuthenticationPrincipal CustomUserDetails로 변경
        UUID userId = UUID.fromString("01a07a54-7f70-7e0d-9a80-15abccab6c92");

        watchPartyParticipantService.leaveWatchParty(partyId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> kick(@PathVariable UUID partyId, @PathVariable UUID userId) {
        //TODO Security 구현 후 @AuthenticationPrincipal CustomUserDetails로 변경
        UUID hostId = UUID.fromString("01a07a54-7f70-7e0d-9a80-15abccab6c92");

        watchPartyParticipantService.kickParticipant(partyId, hostId, userId);
        return ResponseEntity.noContent().build();
    }
}