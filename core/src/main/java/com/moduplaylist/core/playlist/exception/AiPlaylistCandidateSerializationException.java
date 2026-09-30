package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class AiPlaylistCandidateSerializationException extends BaseException {

  public AiPlaylistCandidateSerializationException(Throwable cause) {
    super(ErrorCode.AI_PLAYLIST_CANDIDATE_SERIALIZATION_FAILED, cause);
  }
}