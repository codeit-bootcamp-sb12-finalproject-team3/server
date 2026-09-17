package com.moduplaylist.api.watchparty.controller;

import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.watchparty.service.WatchPartyStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/watch-parties/{partyId}")
@RequiredArgsConstructor
public class WatchPartyStatusController {

    private final WatchPartyStatusService watchPartyStatusService;

    @PatchMapping("/start")
    public ResponseEntity<Void> start(@PathVariable UUID partyId,
                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID hostId = userDetails.getUserId();


        watchPartyStatusService.startWatchParty(partyId, hostId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/end")
    public ResponseEntity<Void> end(@PathVariable UUID partyId,
                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID hostId = userDetails.getUserId();


        watchPartyStatusService.endWatchParty(partyId, hostId);
        return ResponseEntity.noContent().build();
    }
}