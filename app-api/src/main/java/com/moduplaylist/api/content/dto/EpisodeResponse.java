package com.moduplaylist.api.content.dto;

import lombok.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EpisodeResponse {
    private UUID id;
    private Integer episodeNumber;
    private String title;
    private String description;
    private String stillImageUrl;
    private Integer runtime;
    private LocalDate airDate;
}
