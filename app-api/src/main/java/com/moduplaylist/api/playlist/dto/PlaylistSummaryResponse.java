package com.moduplaylist.api.playlist.dto;

import com.moduplaylist.api.content.dto.ContentSummary;
import com.moduplaylist.api.user.dto.UserSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaylistSummaryResponse {

  private UUID id;
  private UserSummary owner;
  private String title;
  private String description;
  private Instant createdAt;
  private long subscriberCount;
  private boolean subscribedByMe;
  private long contentCount;
  private List<ContentSummary> previewContents;


}
