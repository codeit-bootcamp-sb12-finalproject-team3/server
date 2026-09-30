package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class PlaylistMinimumContentException extends BaseException {

  public PlaylistMinimumContentException() {
    super(ErrorCode.PLAYLIST_MINIMUM_CONTENT_REQUIRED);
  }

}
