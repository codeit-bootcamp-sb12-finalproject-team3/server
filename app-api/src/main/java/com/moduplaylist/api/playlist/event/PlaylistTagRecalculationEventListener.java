package com.moduplaylist.api.playlist.event;

import com.moduplaylist.api.playlist.service.PlaylistTaggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaylistTagRecalculationEventListener {

  private final PlaylistTaggingService playlistTaggingService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(PlaylistTagRecalculationEvent event) {
    try {
      playlistTaggingService.recalculate(event.playlistId());
    } catch (Exception e) {
      log.error(
          "플레이리스트 대표 태그 재계산에 실패했습니다. playlistId={}",
          event.playlistId(),
          e
      );
    }
  }
}
