package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchPartyRepository extends JpaRepository<WatchParty, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WatchParty w where w.id = :id")
    Optional<WatchParty> findByIdForUpdate(@Param("id") UUID id);

    // 자동 시작 대상: 아직 SCHEDULED인데 시작 시각이 (since, now] 구간에 있는 파티
    @Query("select w.id from WatchParty w "
            + "where w.status = :status and w.scheduledAt <= :now and w.scheduledAt > :since")
    List<UUID> findIdsToAutoStart(@Param("status") WatchPartyStatus status,
                                  @Param("now") Instant now,
                                  @Param("since") Instant since);

    @Query("""
        select w.id from WatchParty w
        where w.status = :status and w.endedAt < :endedBefore
        order by w.endedAt asc
        """)

    List<UUID> findIdsByStatusAndEndedAtBefore(
            @Param("status") WatchPartyStatus status,
            @Param("endedBefore") Instant endedBefore,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("delete from WatchParty w where w.id in :ids")
    int deleteAllByIdIn(@Param("ids") List<UUID> ids);
}