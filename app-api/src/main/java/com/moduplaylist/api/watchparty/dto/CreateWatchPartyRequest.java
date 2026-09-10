package com.moduplaylist.api.watchparty.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CreateWatchPartyRequest {

    @NotNull
    private UUID contentId;

    @NotBlank
    @Size(max = 100)
    private String title;

    private String description;

    @NotNull
    @Future
    private Instant scheduledAt;

    @NotNull
    @Positive
    private Integer maxParticipants;

    @PositiveOrZero
    private Integer startEpisode;

    @PositiveOrZero
    private Integer endEpisode;

    @AssertTrue(message = "startEpisode/endEpisode는 둘 다 없거나, endEpisode가 startEpisode 이상이어야 합니다.")
    public boolean isEpisodeRangeValid() {
        if (startEpisode == null && endEpisode == null) return true;
        if (startEpisode == null || endEpisode == null) return false;
        return endEpisode >= startEpisode;
    }
}