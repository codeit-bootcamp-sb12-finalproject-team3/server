package com.moduplaylist.api.playlist.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlaylistUpdateRequest {

  @Pattern(
      regexp = "(?s).*\\S.*",
      message = "플레이리스트 제목은 공백일 수 없습니다."
  )
  @Size(
      max = 100,
      message = "플레이리스트 제목은 100자 이하여야 합니다."
  )
  private String title;

  private String description;
}
