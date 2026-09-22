package com.moduplaylist.api.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
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

  @NotNull(message = "플레이리스트 콘텐츠는 필수입니다.")
  @Size(min = 4, message = "플레이리스트에는 최소 4개의 콘텐츠가 필요합니다.")
  private List<UUID> contentIds;
}
