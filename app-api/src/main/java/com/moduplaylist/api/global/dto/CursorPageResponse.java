package com.moduplaylist.api.global.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursorPageResponse<T> {

    private List<T> data;

    private String nextCursor;

    private UUID nextIdAfter;

    private Boolean hasNext;

    private Long totalCount;

    private String sortBy;

    private SortDirection sortDirection;
}