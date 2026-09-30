package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class PlaylistSubscriptionNotFoundException extends BaseException {

  public PlaylistSubscriptionNotFoundException(UUID userId, UUID playlistId) {
    super(ErrorCode.PLAYLIST_SUBSCRIPTION_NOT_FOUND);
    addDetail("userId", userId);
    addDetail("playlistId", playlistId);
  }
}
