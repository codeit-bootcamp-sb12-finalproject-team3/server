package com.moduplaylist.api.watchparty.controller;

import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.watchparty.dto.CreateWatchPartyRequest;
import com.moduplaylist.api.watchparty.dto.WatchPartyResponse;
import com.moduplaylist.api.watchparty.service.WatchPartyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/watch-parties")
@RequiredArgsConstructor
public class WatchPartyController {

    private final WatchPartyService watchPartyService;

    @PostMapping
    public ResponseEntity<WatchPartyResponse> create(@Valid @RequestBody CreateWatchPartyRequest request,
                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID hostId = userDetails.getUserId();

        WatchPartyResponse response = watchPartyService.createWatchParty(hostId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}