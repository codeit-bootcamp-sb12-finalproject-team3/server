package com.moduplaylist.core.playlist.ai;

import com.moduplaylist.core.content.entity.ContentType;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class AiPlaylistCandidate {

  private final UUID contentId;
  private final String title;
  private final String description;
  private final ContentType type;
  private final List<String> tags;

  public AiPlaylistCandidate(
      UUID contentId,
      String title,
      String description,
      ContentType type,
      List<String> tags
  ) {
    this.contentId = contentId;
    this.title = title;
    this.description = description;
    this.type = type;
    this.tags = tags;
  }
}
