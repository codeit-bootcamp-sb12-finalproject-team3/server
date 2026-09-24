package com.moduplaylist.api.watchparty.controller;

import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.watchparty.dto.WatchPartyParticipantResponse;
import com.moduplaylist.api.watchparty.service.WatchPartyParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/watch-parties/{partyId}/participants")
@RequiredArgsConstructor
public class WatchPartyParticipantController {

    private final WatchPartyParticipantService watchPartyParticipantService;

    @GetMapping
    public ResponseEntity<List<WatchPartyParticipantResponse>> getParticipants(
            @PathVariable UUID partyId
    ) {
        return ResponseEntity.ok(watchPartyParticipantService.getParticipants(partyId));
    }

    @PostMapping
    public ResponseEntity<Void> join(@PathVariable UUID partyId,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails.getUserId();

        watchPartyParticipantService.joinWatchParty(partyId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> leave(@PathVariable UUID partyId,
                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails.getUserId();

        watchPartyParticipantService.leaveWatchParty(partyId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> kick(@PathVariable UUID partyId,
                                     @PathVariable UUID userId,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID hostId = userDetails.getUserId();


        watchPartyParticipantService.kickParticipant(partyId, hostId, userId);
        return ResponseEntity.noContent().build();
    }
}