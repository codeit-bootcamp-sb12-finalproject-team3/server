package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.Playlist;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
      UPDATE playlists p
      LEFT JOIN (
        SELECT
          playlist_id,
          COUNT(*) AS total_subscriber_count,
          SUM(
            CASE
              WHEN created_at >= :weekStart THEN 1
              ELSE 0
            END
          ) AS weekly_new_subscriber_count
        FROM playlist_subscriptions
        GROUP BY playlist_id
      ) s ON s.playlist_id = p.id
      SET p.weekly_popularity_score = ROUND(
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
}
