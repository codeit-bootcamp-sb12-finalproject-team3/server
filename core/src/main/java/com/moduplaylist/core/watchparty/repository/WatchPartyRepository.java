package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.entity.WatchParty;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface WatchPartyRepository extends JpaRepository<WatchParty, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WatchParty w where w.id = :id")
    Optional<WatchParty> findByIdForUpdate(UUID id);
}