package com.moduplaylist.api.content.dto;

import lombok.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentListRequest {
    @Pattern(regexp = "movie|tvSeries|tvSeason|sport")
    private String typeEqual;
    private UUID genreIdEqual;
    @Size(max = 50)
    private String sportTypeEqual;
    private String keywordLike;
    private UUID likedByUserIdEqual;
    private String cursor;
    private UUID idAfter;
    @Builder.Default
    @NotNull @Min(1) @Max(100)
    private Integer limit = 20;
    @Builder.Default
    @NotNull @Pattern(regexp = "createdAt|averageRating")
    private String sortBy = "createdAt";
    @Builder.Default
    @NotNull
    private com.moduplaylist.api.global.dto.SortDirection sortDirection =
            com.moduplaylist.api.global.dto.SortDirection.DESCENDING;
}
