package com.moduplaylist.api.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlaylistCreateRequest {

  @NotBlank(message = "플레이리스트 제목은 필수입니다.")
  @Size(max = 100)
  private String title;

  @NotBlank(message = "플레이리스트 설명은 필수입니다.")
  private String description;
}
