package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class PlaylistNotFoundException extends BaseException {

  public PlaylistNotFoundException(UUID playlistId) {
    super(ErrorCode.PLAYLIST_NOT_FOUND);
    addDetail("playlistId", playlistId);
  }
}
