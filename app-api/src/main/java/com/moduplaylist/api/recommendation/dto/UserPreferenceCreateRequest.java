package com.moduplaylist.api.recommendation.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPreferenceCreateRequest {
    @Size(min = 3)
    private List<UUID> contentIds;
}
