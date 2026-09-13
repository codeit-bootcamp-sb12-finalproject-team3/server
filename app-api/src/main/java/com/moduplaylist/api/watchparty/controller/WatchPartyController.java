package com.moduplaylist.api.watchparty.controller;

import com.moduplaylist.api.watchparty.dto.CreateWatchPartyRequest;
import com.moduplaylist.api.watchparty.dto.WatchPartyResponse;
import com.moduplaylist.api.watchparty.service.WatchPartyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/watch-parties")
@RequiredArgsConstructor
public class WatchPartyController {

    private final WatchPartyService watchPartyService;

    @PostMapping
    public ResponseEntity<WatchPartyResponse> create(@Valid @RequestBody CreateWatchPartyRequest request) {
        //TODO Security 구현 후 @AuthenticationPrincipal CustomUserDetails로 변경
        UUID hostId = UUID.fromString("01a07a54-7f70-7e0d-9a80-15abccab6c92");

        WatchPartyResponse response = watchPartyService.createWatchParty(hostId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}