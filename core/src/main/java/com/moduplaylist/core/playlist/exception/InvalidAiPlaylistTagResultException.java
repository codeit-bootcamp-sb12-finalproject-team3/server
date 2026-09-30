package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class InvalidAiPlaylistTagResultException extends BaseException {

  public InvalidAiPlaylistTagResultException(String reason) {
    super(ErrorCode.AI_PLAYLIST_INVALID_TAG_RESULT);
    addDetail("reason", reason);
  }
}