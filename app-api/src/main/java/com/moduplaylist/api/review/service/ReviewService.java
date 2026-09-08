package com.moduplaylist.api.review.service;

import com.moduplaylist.api.review.dto.*;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public interface ReviewService {
    ReviewResponse findById(@NotNull UUID id, boolean includeSpoilers);
    CursorPageResponse<ReviewResponse> findAll(@Valid @NotNull ReviewListRequest request);
    ReviewResponse create(@Valid @NotNull ReviewCreateRequest request);
    ReviewResponse update(@NotNull UUID id, @Valid @NotNull ReviewUpdateRequest request);
    void delete(@NotNull UUID id);
}
