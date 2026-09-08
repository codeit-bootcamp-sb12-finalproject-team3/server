package com.moduplaylist.api.review.dto;

import lombok.*;

import com.moduplaylist.api.global.dto.SortDirection;
import jakarta.validation.constraints.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewListRequest {
    private UUID contentIdEqual;
    private UUID userIdEqual;
    private boolean includeSpoilers;
    private String cursor;
    private UUID idAfter;
    @Builder.Default
    @NotNull @Min(1) @Max(100)
    private Integer limit = 20;
    @Builder.Default
    @NotNull @Pattern(regexp = "createdAt")
    private String sortBy = "createdAt";
    @Builder.Default
    @NotNull
    private SortDirection sortDirection = SortDirection.DESCENDING;
}
