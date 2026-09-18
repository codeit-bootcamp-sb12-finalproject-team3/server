package com.moduplaylist.core.playlist.repository;

import com.moduplaylist.core.playlist.entity.Playlist;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

  // 동일 플레이리스트의 콘텐츠 삭제 요청을 직렬화
  // -> 동시 삭제로 콘텐츠 수가 최소 4개 미만이 되는 것을 방지한다.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Playlist p where p.id = :id")
  Optional<Playlist> findByIdForUpdate(@Param("id") UUID id);
}
