package com.moduplaylist.api.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiPlaylistCreateRequest {

  @NotBlank(message = "플레이리스트 테마는 필수입니다.")
  @Size(max = 200, message = "플레이리스트 테마는 200자 이하여야 합니다.")
  private String theme;

}
