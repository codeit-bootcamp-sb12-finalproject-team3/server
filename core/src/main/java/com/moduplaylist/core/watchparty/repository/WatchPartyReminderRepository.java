package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.entity.WatchPartyReminder;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WatchPartyReminderRepository extends JpaRepository<WatchPartyReminder, UUID> {
    boolean existsByWatchParty_IdAndUser_Id(UUID watchPartyId, UUID userId);

    @Query("""
            select r from WatchPartyReminder r
            join fetch r.watchParty w
            join fetch r.user u
            where w.status = :status
            and w.scheduledAt <= :threshold
            """)
    List<WatchPartyReminder> findDueReminders(
            @Param("status") WatchPartyStatus status,
            @Param("threshold") Instant threshold);
}