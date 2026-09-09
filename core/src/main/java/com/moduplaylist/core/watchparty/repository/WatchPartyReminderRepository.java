package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.entity.WatchPartyReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WatchPartyReminderRepository extends JpaRepository<WatchPartyReminder, UUID> {
    boolean existsByWatchParty_IdAndUser_Id(UUID watchPartyId, UUID userId);
}