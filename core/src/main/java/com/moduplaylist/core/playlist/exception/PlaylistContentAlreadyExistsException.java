package com.moduplaylist.core.playlist.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class PlaylistContentAlreadyExistsException extends BaseException {

  // 서비스에서 중복 콘텐츠를 발견한 경우
  public PlaylistContentAlreadyExistsException(UUID playlistId, UUID contentId) {
    this(playlistId, contentId, null);
  }

  // 동시 요청으로 DB UNIQUE 제약이 발생한 경우
  public PlaylistContentAlreadyExistsException(
      UUID playlistId,
      UUID contentId,
      Throwable cause
  ) {
    super(ErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS, cause);
    addDetail("playlistId", playlistId);
    addDetail("contentId", contentId);
  }
}
