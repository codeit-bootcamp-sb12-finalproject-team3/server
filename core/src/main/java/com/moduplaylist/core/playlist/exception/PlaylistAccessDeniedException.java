package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class PlaylistAccessDeniedException extends BaseException {

  public PlaylistAccessDeniedException(UUID playlistId) {
    super(ErrorCode.PLAYLIST_ACCESS_DENIED);
    addDetail("playlistId", playlistId);
  }

}
