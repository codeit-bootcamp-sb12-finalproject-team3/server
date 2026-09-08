package com.moduplaylist.core.watchparty.repository;

import com.moduplaylist.core.watchparty.WatchParty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WatchPartyRepository extends JpaRepository<WatchParty, UUID> {
}