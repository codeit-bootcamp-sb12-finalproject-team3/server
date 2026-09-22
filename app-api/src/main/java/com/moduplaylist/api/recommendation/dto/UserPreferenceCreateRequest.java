package com.moduplaylist.api.recommendation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPreferenceCreateRequest {

    @NotNull(message = "contentIds는 필수입니다.")
    @Size(min = 3, message = "contentIds는 최소 3개 이상이어야 합니다.")
    @UniqueElements(message = "contentIds에는 중복된 값이 있을 수 없습니다.")
    private List<@NotNull(message = "contentId는 null일 수 없습니다.") UUID> contentIds;
}
