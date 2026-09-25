package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class InvalidAiPlaylistContentResultException extends BaseException {

  public InvalidAiPlaylistContentResultException(String reason) {
    super(ErrorCode.AI_PLAYLIST_INVALID_CONTENT_RESULT);
    addDetail("reason", reason);
  }
}