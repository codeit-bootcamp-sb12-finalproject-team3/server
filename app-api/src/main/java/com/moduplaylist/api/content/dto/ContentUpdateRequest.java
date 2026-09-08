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
/**
 * 부분 수정 요청. null은 유지, 빈 tags 목록은 수동 태그 전체 제거를 의미한다.
 * 타입 변경 가능 여부는 기존 시즌 관계를 조회하는 Service에서 검증한다.
 */
public class ContentUpdateRequest {
    @Size(max = 255) @Pattern(regexp = "(?s).*\\S.*")
    private String title;
    @Pattern(regexp = "movie|tvSeries|tvSeason|sport")
    private String type;
    private String description;
    @Size(max = 500)
    private String thumbnailUrl;
    private List<@NotBlank @Size(max = 100) String> tags;
}
