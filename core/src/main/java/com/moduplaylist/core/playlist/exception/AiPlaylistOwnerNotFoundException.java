package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class AiPlaylistOwnerNotFoundException extends BaseException {

  public AiPlaylistOwnerNotFoundException(String email) {
    super(ErrorCode.AI_PLAYLIST_OWNER_NOT_FOUND);
    addDetail("email", email);
  }

}
