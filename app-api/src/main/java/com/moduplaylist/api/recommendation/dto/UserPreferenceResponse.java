package com.moduplaylist.api.recommendation.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferenceResponse {

    private List<UUID> contentIds;
}