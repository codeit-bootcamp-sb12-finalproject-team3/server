package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.PlaylistSubscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistSubscriptionRepository extends JpaRepository<PlaylistSubscription, UUID> {

  long countByPlaylist_Id(UUID playlistId);

  // 현재 로그인 사용자의 플레이리스트 구독 여부
  boolean existsByUser_IdAndPlaylist_Id(UUID userId, UUID playlistId);

  List<PlaylistSubscription> findAllByUser_IdAndPlaylist_IdIn(
      UUID userId,
      List<UUID> playlistIds
  );

  Optional<PlaylistSubscription> findByUser_IdAndPlaylist_Id(
      UUID userId,
      UUID playlistId
  );

  @Query("""
      select subscription.user.id
      from PlaylistSubscription subscription
      where subscription.playlist.id = :playlistId
      """)
  List<UUID> findSubscriberIdsByPlaylistId(@Param("playlistId") UUID playlistId);

  @Query("""
      select subscription.playlist.id
      from PlaylistSubscription subscription
      where subscription.user.id = :userId
      """)
  List<UUID> findPlaylistIdsByUserId(@Param("userId") UUID userId);

}
