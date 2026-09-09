package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.WatchPartyParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WatchPartyParticipantRepository extends JpaRepository<WatchPartyParticipant, UUID> {
    boolean existsByUser_IdAndWatchParty_Id(UUID userId, UUID watchPartyId);
}