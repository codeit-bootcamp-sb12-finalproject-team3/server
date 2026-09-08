package com.moduplaylist.api.content.service;

import com.moduplaylist.api.content.dto.*;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface ContentService {
    ContentDetailResponse findById(@NotNull UUID id);
    CursorPageResponse<ContentSummaryResponse> findAll(@Valid @NotNull ContentListRequest request);
    ContentDetailResponse create(@Valid @NotNull ContentCreateRequest request);
    ContentDetailResponse update(@NotNull UUID id, @Valid @NotNull ContentUpdateRequest request);
    void delete(@NotNull UUID id);
}
