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
public class ContentCreateRequest {
    @NotBlank @Size(max = 255)
    private String title;
    @NotNull @Pattern(regexp = "movie|tvSeries|tvSeason|sport")
    private String type;
    private String description;
    @Size(max = 500)
    private String thumbnailUrl;
    private UUID parentContentId;
    @PositiveOrZero
    private Integer seasonNumber;
    @PositiveOrZero
    private Integer seasonCount;
    @PositiveOrZero
    private Integer episodeCount;
    @Size(max = 50)
    private String sportType;
    private LocalDate releaseDate;
    @Positive
    private Integer runtime;
    private Map<String, Object> metadata;
    private List<@NotBlank @Size(max = 100) String> tags;

    @AssertTrue(message = "콘텐츠 타입에 맞는 시즌 정보와 스포츠 종목을 지정해야 합니다.")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isTypeFieldsValid() {
        if (type == null) return true; // 필수값 검증은 @NotNull이 담당한다.
        boolean seasonValid = "tvSeason".equals(type)
                ? parentContentId != null && seasonNumber != null
                : parentContentId == null && seasonNumber == null && episodeCount == null;
        boolean seriesValid = "tvSeries".equals(type) || seasonCount == null;
        boolean sportValid = "sport".equals(type)
                ? sportType != null && !sportType.isBlank() : sportType == null;
        return seasonValid && seriesValid && sportValid;
    }
}
