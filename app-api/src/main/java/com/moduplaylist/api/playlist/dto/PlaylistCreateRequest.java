package com.moduplaylist.api.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlaylistCreateRequest {

  @NotBlank
  @Size(max = 100)
  private String title;

  private String description;
}
