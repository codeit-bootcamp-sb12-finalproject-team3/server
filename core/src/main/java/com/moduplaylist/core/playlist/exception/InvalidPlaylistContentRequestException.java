package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class InvalidPlaylistContentRequestException extends BaseException {

  public InvalidPlaylistContentRequestException() {
    super(ErrorCode.INVALID_PLAYLIST_CONTENT_REQUEST);
  }

}
