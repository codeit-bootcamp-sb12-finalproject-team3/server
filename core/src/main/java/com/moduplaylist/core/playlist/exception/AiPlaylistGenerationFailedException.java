package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class AiPlaylistGenerationFailedException extends BaseException {

  public AiPlaylistGenerationFailedException() {
    super(ErrorCode.AI_PLAYLIST_GENERATION_FAILED);
  }

  public AiPlaylistGenerationFailedException(Throwable cause) {
    super(ErrorCode.AI_PLAYLIST_GENERATION_FAILED, cause);
  }
}