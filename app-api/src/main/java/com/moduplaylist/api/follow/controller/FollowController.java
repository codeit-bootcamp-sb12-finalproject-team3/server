package com.moduplaylist.api.follow.controller;

import com.moduplaylist.api.follow.dto.FollowDto;
import com.moduplaylist.api.follow.dto.FollowRequest;
import com.moduplaylist.api.follow.service.FollowService;
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
            @Valid @RequestBody FollowRequest request
            ) {
        // TODO Security 구현 후
        // @AuthenticationPrincipal CustomUserDetails userDetails 로 변경
        // UUID followerId = userDetails.getUserId();

        UUID followerId = UUID.fromString(
                "01a07a54-7f70-7e0d-9a80-15abccab6c92");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(followService.create(followerId, request.getFolloweeId()));
    }

    @DeleteMapping("/{followId}")
    public ResponseEntity<Void>  delete(@PathVariable UUID followId) {
        // TODO Security 구현 후
        // @AuthenticationPrincipal CustomUserDetails userDetails 로 변경
        // UUID followerId = userDetails.getUserId();

        UUID followerId = UUID.fromString(
                "01a07a54-7f70-7e0d-9a80-15abccab6c92");

        followService.delete(followerId, followId);
        return ResponseEntity.noContent().build();
    }

}
