package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchPartyParticipantRepository extends JpaRepository<WatchPartyParticipant, UUID> {
    boolean existsByUser_IdAndWatchParty_Id(UUID userId, UUID watchPartyId);
    Optional<WatchPartyParticipant> findByUser_IdAndWatchParty_Id(UUID userId, UUID watchPartyId);

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

    interface ParticipantCount {
        UUID getWatchPartyId();
        long getCount();
    }

    long countByWatchParty_IdAndStatus(UUID watchPartyId, ParticipantStatus status);

    boolean existsByUser_IdAndStatusAndWatchParty_IdNotAndWatchParty_StatusNot(
            UUID userId,
            ParticipantStatus status,
            UUID watchPartyId,
            WatchPartyStatus watchPartyStatus
    );

}