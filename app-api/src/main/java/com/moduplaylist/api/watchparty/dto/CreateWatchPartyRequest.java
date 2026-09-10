package com.moduplaylist.api.watchparty.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
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
}