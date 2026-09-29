package com.moduplaylist.api.watchparty.controller;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.watchparty.dto.CreateWatchPartyRequest;
import com.moduplaylist.api.watchparty.dto.UpdateWatchPartyRequest;
import com.moduplaylist.api.watchparty.dto.WatchPartyResponse;
import com.moduplaylist.api.watchparty.dto.WatchPartySummaryResponse;
import com.moduplaylist.api.watchparty.service.WatchPartyService;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartySearch;
import jakarta.validation.Valid;
import java.util.List;
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
    public ResponseEntity<WatchPartyResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateWatchPartyRequest request
    ) {
        WatchPartyResponse response = watchPartyService.createWatchParty(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{partyId}")
    public ResponseEntity<WatchPartyResponse> findById(@PathVariable UUID partyId) {
        return ResponseEntity.ok(watchPartyService.getWatchParty(partyId));
    }

    @GetMapping
    public ResponseEntity<CursorPageResponse<WatchPartySummaryResponse>> findAll(
            @RequestParam(required = false) WatchPartyStatus statusEqual,
            @RequestParam(required = false) UUID contentIdEqual,
            @RequestParam(required = false) String keywordLike,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam int limit,
            @RequestParam String sortDirection
    ) {
        WatchPartySearch.Sort sort = parseSort(sortBy);
        SortDirection direction = parseDirection(sortDirection);
        return ResponseEntity.ok(
                watchPartyService.getWatchParties(
                        statusEqual, contentIdEqual, keywordLike, sort, cursor, idAfter, limit, direction)
        );
    }

    @PatchMapping("/{partyId}")
    public ResponseEntity<WatchPartyResponse> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID partyId,
            @Valid @RequestBody UpdateWatchPartyRequest request
    ) {
        return ResponseEntity.ok(
                watchPartyService.updateWatchParty(userDetails.getUserId(), partyId, request)
        );
    }

    @DeleteMapping("/{partyId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID partyId
    ) {
        watchPartyService.deleteWatchParty(userDetails.getUserId(), partyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/scheduled-by-me")
    public ResponseEntity<List<WatchPartySummaryResponse>> findScheduledByMe(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
            watchPartyService.getScheduledWatchParties(userDetails.getUserId())
        );
    }

    // URL의 sortBy 문자열 → 정렬 enum. 안 보내면 기존 동작(시작 시각순)
    private WatchPartySearch.Sort parseSort(String sortBy) {
        if (sortBy == null) {
            return WatchPartySearch.Sort.SCHEDULED_AT;
        }
        return switch (sortBy) {
            case "scheduledAt" -> WatchPartySearch.Sort.SCHEDULED_AT;
            case "participantCount" -> WatchPartySearch.Sort.PARTICIPANT_COUNT;
            default -> throw new BaseException(ErrorCode.INVALID_REQUEST);
        };
    }

    private SortDirection parseDirection(String sortDirection) {
        try {
            return SortDirection.valueOf(sortDirection);
        } catch (IllegalArgumentException e) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, e);
        }
    }
}