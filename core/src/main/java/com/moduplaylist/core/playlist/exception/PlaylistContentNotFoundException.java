package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class PlaylistContentNotFoundException extends BaseException {

  public PlaylistContentNotFoundException(UUID playlistId, UUID contentId) {
    super(ErrorCode.PLAYLIST_CONTENT_NOT_FOUND);
    addDetail("playlistId", playlistId);
    addDetail("contentId", contentId);
  }
}
