package com.moduplaylist.api.review.controller;

import com.moduplaylist.api.review.dto.*;
import com.moduplaylist.api.review.service.ReviewService;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService service;
    @GetMapping
    public CursorPageResponse<ReviewResponse> findAll(@Valid @ModelAttribute ReviewListRequest request) {
        return service.findAll(request);
    }
    @GetMapping("/{reviewId}")
    public ReviewResponse findById(@PathVariable UUID reviewId,
            @RequestParam(defaultValue = "false") boolean includeSpoilers) {
        return service.findById(reviewId, includeSpoilers);
    }
    @PostMapping
    public ResponseEntity<ReviewResponse> create(@Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/reviews/" + response.getId())).body(response);
    }
    @PatchMapping("/{reviewId}")
    public ReviewResponse update(@PathVariable UUID reviewId, @Valid @RequestBody ReviewUpdateRequest request) {
        return service.update(reviewId, request);
    }
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(@PathVariable UUID reviewId) {
        service.delete(reviewId);
        return ResponseEntity.noContent().build();
    }
}
