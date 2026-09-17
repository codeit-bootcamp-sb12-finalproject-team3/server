package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class SelfPlaylistSubscriptionNotAllowedException extends BaseException {

  public SelfPlaylistSubscriptionNotAllowedException(UUID userId, UUID playlistId) {
    super(ErrorCode.SELF_PLAYLIST_SUBSCRIPTION_NOT_ALLOWED);
    addDetail("userId", userId);
    addDetail("playlistId", playlistId);
  }
}
