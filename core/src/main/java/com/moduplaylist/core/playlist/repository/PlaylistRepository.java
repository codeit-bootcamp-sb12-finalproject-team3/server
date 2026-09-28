package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.Playlist;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

  @Query("select playlist.id from Playlist playlist")
  List<UUID> findAllIds();

  // 동일 플레이리스트의 콘텐츠 삭제 요청을 직렬화
  // -> 동시 삭제로 콘텐츠 수가 최소 4개 미만이 되는 것을 방지한다.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Playlist p where p.id = :id")
  Optional<Playlist> findByIdForUpdate(@Param("id") UUID id);

  // 모든 플레이리스트의 주간 인기 점수를 현재 구독 상태 기준으로 일괄 재계산한다.
  @Modifying
  @Query(
    value = """
      update playlists p
      left join (
        select 
          playlist_id,
          COUNT(*) as total_subscriber_count,
          SUM(
            case 
              when created_at >= :weekStart then 1
              else 0
            end
          ) as weekly_new_subscriber_count
        from playlist_subscriptions
        group by playlist_id
      ) s on s.playlist_id = p.id
      set p.weekly_popularity_score = ROUND(
        COALESCE(s.total_subscriber_count, 0) * :totalSubscriberWeight
        + COALESCE(s.weekly_new_subscriber_count, 0) * :weeklyNewSubscriberWeight,
        2
      )
      """,
      nativeQuery = true)
  int updateWeeklyPopularityScores(
      @Param("weekStart") Instant weekStart,
      @Param("totalSubscriberWeight") BigDecimal totalSubscriberWeight,
      @Param("weeklyNewSubscriberWeight") BigDecimal weeklyNewSubscriberWeight);

  @Query("""
    select playlist.title
    from Playlist playlist
    where playlist.owner.id = :ownerId
    order by playlist.createdAt desc
    """)
  List<String> findRecentTitlesByOwnerId(
      @Param("ownerId") UUID ownerId,
      Pageable pageable
  );

  boolean existsByOwner_IdAndTitle(UUID ownerId, String title);

  @Query("""
    select count(playlist)
    from Playlist playlist
    where playlist.owner.id = :ownerId
      and playlist.createdAt >= :weekStart
      and playlist.createdAt < :nextWeekStart
    """)
  long countCreatedByOwnerInWeek(
      @Param("ownerId") UUID ownerId,
      @Param("weekStart") Instant weekStart,
      @Param("nextWeekStart") Instant nextWeekStart
  );
}
