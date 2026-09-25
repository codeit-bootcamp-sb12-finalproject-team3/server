package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class InvalidAiPlaylistDescriptionException extends BaseException {

  public InvalidAiPlaylistDescriptionException(String reason) {
    super(ErrorCode.AI_PLAYLIST_INVALID_DESCRIPTION);
    addDetail("reason", reason);
  }
}