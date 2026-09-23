package com.moduplaylist.api.watchparty.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class UpdateWatchPartyRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    @Future
    private Instant scheduledAt;

    @NotNull
    @Positive
    private Integer maxParticipants;

    @NotNull
    @Positive
    private Integer sessionDurationMinutes;

    private Integer startEpisode;

    private Integer endEpisode;

    @AssertTrue(message = "startEpisode/endEpisode는 둘 다 있거나 둘 다 없어야 합니다.")
    public boolean isEpisodeRangeValid() {
        return (startEpisode == null) == (endEpisode == null);
    }
}