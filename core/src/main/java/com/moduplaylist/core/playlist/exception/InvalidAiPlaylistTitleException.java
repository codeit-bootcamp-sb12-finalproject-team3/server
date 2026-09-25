package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class InvalidAiPlaylistTitleException extends BaseException {

  public InvalidAiPlaylistTitleException(String reason) {
    super(ErrorCode.AI_PLAYLIST_INVALID_TITLE);
    addDetail("reason", reason);
  }
}