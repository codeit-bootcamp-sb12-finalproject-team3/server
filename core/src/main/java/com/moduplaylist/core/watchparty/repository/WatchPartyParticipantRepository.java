package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WatchPartyParticipantRepository extends JpaRepository<WatchPartyParticipant, UUID> {
    boolean existsByUser_IdAndWatchParty_Id(UUID userId, UUID watchPartyId);
    Optional<WatchPartyParticipant> findByUser_IdAndWatchParty_Id(UUID userId, UUID watchPartyId);

    long countByWatchParty_IdAndStatus(UUID watchPartyId, ParticipantStatus status);
}