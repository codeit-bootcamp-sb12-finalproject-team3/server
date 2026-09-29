package com.moduplaylist.api.watchparty.controller;

import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.watchparty.service.WatchPartyReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("api/watch-parties/{partyId}/reminders")
@RequiredArgsConstructor
public class WatchPartyReminderController {

    private final WatchPartyReminderService watchPartyReminderService;

    @PostMapping
    public ResponseEntity<Void> setReminder(@PathVariable UUID partyId,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        watchPartyReminderService.setReminder(partyId, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> cancelReminder(@PathVariable UUID partyId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        watchPartyReminderService.cancelReminder(partyId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}