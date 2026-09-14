package com.moduplaylist.api.follow.controller;

import com.moduplaylist.api.follow.dto.FollowDto;
import com.moduplaylist.api.follow.dto.FollowRequest;
import com.moduplaylist.api.follow.service.FollowService;
import com.moduplaylist.api.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/follows")
@RequiredArgsConstructor
public class FollowController {
    private final FollowService followService;

    @PostMapping()
    public ResponseEntity<FollowDto> create(
            @Valid @RequestBody FollowRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID followerId = userDetails.getUserId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(followService.create(followerId, request.getFolloweeId()));
    }

    @DeleteMapping("/{followeeId}") /// 스웨거 명세와 달라 추후 프론트 작업 필수!!!!
    public ResponseEntity<Void>  delete(
            @PathVariable UUID followeeId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID followerId = userDetails.getUserId();

        followService.delete(followerId, followeeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/followed-by-me")
    public ResponseEntity<FollowDto> getFollow(
            @RequestParam UUID followeeId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID followerId = userDetails.getUserId();

        return ResponseEntity.ok(followService.getFollow(followerId, followeeId));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getFollowerCount(@RequestParam UUID followeeId) {
        return ResponseEntity.ok(followService.getFollowerCount(followeeId));
    }
}
