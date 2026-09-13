package com.moduplaylist.api.content.dto;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentSummary {

  private UUID id;
  private ContentType type;
  private String title;
  private String description;
  private String thumbnailUrl;
  private List<String> tags;
  private BigDecimal averageRaging;
  private long reviewCount;

  public static ContentSummary from(Content content) {
    return ContentSummary.builder()
        .id(content.getId())
        .type(content.getType())
        .title(content.getTitle())
        .description(content.getDescription())
        .thumbnailUrl(content.getThumbnailUrl())
        .tags(Collections.emptyList())
        .averageRaging(content.getAverageRating())
        .reviewCount(content.getReviewCount())
        .build();
  }
}
