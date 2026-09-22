package com.moduplaylist.core.playlist.policy;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.exception.ContentTypeNotSupportedException;
import com.moduplaylist.core.playlist.exception.PlaylistMinimumContentException;

public final class PlaylistContentPolicy {

  public static final int MIN_CONTENT_COUNT = 4;

  private PlaylistContentPolicy() {
  }

  public static void validateContent(Content content) {
    if (content.isHidden()) {
      throw new ContentNotFoundException(content.getId());
    }

    if (!content.getType().isPersonalizable()) {
      throw new ContentTypeNotSupportedException(
          content.getId(),
          content.getType()
      );
    }
  }

  public static void validateMinimumContentCount(int contentCount) {
    if (contentCount < MIN_CONTENT_COUNT) {
      throw new PlaylistMinimumContentException();
    }
  }

  public static void validateRemoval(
      Content content,
      long visibleContentCount
  ) {
    if (!content.isHidden()
        && visibleContentCount <= MIN_CONTENT_COUNT) {
      throw new PlaylistMinimumContentException();
    }
  }
}
