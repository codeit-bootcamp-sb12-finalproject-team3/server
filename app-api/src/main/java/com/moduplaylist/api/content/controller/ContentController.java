package com.moduplaylist.api.content.controller;

import com.moduplaylist.api.content.dto.*;
import com.moduplaylist.api.content.service.ContentService;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents")
public class ContentController {
    private final ContentService service;

    @GetMapping
    public CursorPageResponse<ContentSummaryResponse> findAll(@Valid @ModelAttribute ContentListRequest request) {
        return service.findAll(request);
    }

    @GetMapping("/{contentId}")
    public ContentDetailResponse findById(@PathVariable UUID contentId) {
        return service.findById(contentId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContentDetailResponse> create(@Valid @RequestBody ContentCreateRequest request) {
        ContentDetailResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/contents/" + response.getId())).body(response);
    }

    @PatchMapping("/{contentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ContentDetailResponse update(@PathVariable UUID contentId, @Valid @RequestBody ContentUpdateRequest request) {
        return service.update(contentId, request);
    }

    @DeleteMapping("/{contentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID contentId) {
        service.delete(contentId);
        return ResponseEntity.noContent().build();
    }
}
