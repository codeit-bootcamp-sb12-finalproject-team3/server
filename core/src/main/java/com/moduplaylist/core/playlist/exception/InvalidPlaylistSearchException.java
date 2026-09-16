package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class InvalidPlaylistSearchException extends BaseException {

  public InvalidPlaylistSearchException(String reason) {
    super(ErrorCode.INVALID_PLAYLIST_SEARCH);
    addDetail("reason", reason);
  }
}
