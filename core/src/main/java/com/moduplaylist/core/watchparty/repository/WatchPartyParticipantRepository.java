package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchPartyParticipantRepository extends JpaRepository<WatchPartyParticipant, UUID> {
    boolean existsByUser_IdAndWatchParty_Id(UUID userId, UUID watchPartyId);
    Optional<WatchPartyParticipant> findByUser_IdAndWatchParty_Id(UUID userId, UUID watchPartyId);

    Optional<WatchPartyParticipant> findFirstByUser_IdAndStatusAndWatchParty_IdNotAndWatchParty_StatusNot(
            UUID userId,
            ParticipantStatus status,
            UUID watchPartyId,
            WatchPartyStatus watchPartyStatus
    );

    @Query("select wpp.watchParty.id as watchPartyId, count(wpp) as count " +
            "from WatchPartyParticipant wpp " +
            "where wpp.watchParty.id in :watchPartyIds and wpp.status = :status " +
            "group by wpp.watchParty.id")
    List<ParticipantCount> countByWatchPartyIdsAndStatus(
            @Param("watchPartyIds") List<UUID> watchPartyIds,
            @Param("status") ParticipantStatus status);

    @Query("select wpp from WatchPartyParticipant wpp " +
            "join fetch wpp.user " +
            "where wpp.watchParty.id = :watchPartyId and wpp.status = :status " +
            "order by wpp.joinedAt asc")
    List<WatchPartyParticipant> findJoinedParticipants(
            @Param("watchPartyId") UUID watchPartyId,
            @Param("status") ParticipantStatus status);

    // 동시 정리 방지: JOINED일 때만 LEFT로 바꾼다. 반환값 = 바뀐 행 수(0 또는 1)
    // 1일 때만 이벤트·Redis 정리를 진행해야 LEFT 방송이 중복되지 않는다.
    @Modifying
    @Query("update WatchPartyParticipant wpp " +
            "set wpp.status = com.moduplaylist.core.watchparty.entity.ParticipantStatus.LEFT, " +
            "    wpp.leftAt = :leftAt " +
            "where wpp.id = :id " +
            "and wpp.status = com.moduplaylist.core.watchparty.entity.ParticipantStatus.JOINED")
    int markLeftIfJoined(@Param("id") UUID id, @Param("leftAt") Instant leftAt);

    // 스케줄러: 종료되지 않은 파티 중 JOINED 참가자가 있는 파티 ID
    @Query("select distinct wpp.watchParty.id from WatchPartyParticipant wpp " +
            "where wpp.status = :status and wpp.watchParty.status <> :excludedPartyStatus")
    List<UUID> findPartyIdsHavingParticipantStatus(
            @Param("status") ParticipantStatus status,
            @Param("excludedPartyStatus") WatchPartyStatus excludedPartyStatus);

    interface ParticipantCount {
        UUID getWatchPartyId();
        long getCount();
    }

    long countByWatchParty_IdAndStatus(UUID watchPartyId, ParticipantStatus status);

}