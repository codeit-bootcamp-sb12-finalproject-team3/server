package com.moduplaylist.core.playlist.ai;

import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class AiPlaylistGenerationResult {

  private final String title;
  private final String description;
  private final List<UUID> contentIds;
  private final List<String> tags;

  public AiPlaylistGenerationResult(
      String title,
      String description,
      List<UUID> contentIds,
      List<String> tags
  ) {
    this.title = title;
    this.description = description;
    this.contentIds = contentIds == null ? List.of() : List.copyOf(contentIds);
    this.tags = tags == null ? List.of() : List.copyOf(tags);
  }
}
