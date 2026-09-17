package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class PlaylistAlreadySubscribedException extends BaseException {

  // 서비스에서 중복 구독을 발견한 경우
  public PlaylistAlreadySubscribedException(UUID userId, UUID playlistId) {
    this(userId, playlistId, null);
  }

  // 동시 요청으로 DB UNIQUE 제약이 발생한 경우
  public PlaylistAlreadySubscribedException(
      UUID userId,
      UUID playlistId,
      Throwable cause
  ) {
    super(ErrorCode.PLAYLIST_ALREADY_SUBSCRIBED, cause);
    addDetail("userId", userId);
    addDetail("playlistId", playlistId);
  }
}
