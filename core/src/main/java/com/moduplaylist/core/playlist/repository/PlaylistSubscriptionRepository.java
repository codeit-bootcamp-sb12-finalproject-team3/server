package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.PlaylistSubscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistSubscriptionRepository extends JpaRepository<PlaylistSubscription, UUID> {

  long CountByPlaylist_Id(UUID playlistId);

  // 현재 로그인 사용자의 플레이리스트 구독 여부
  boolean existsByPlaylist_IdAndUser_Id(UUID playlistId, UUID userId);

}
