package com.moduplaylist.core.user.repository;

import java.time.Duration;
import java.util.UUID;

public interface TemporaryPasswordStore {

  void save(UUID userId, String encodedPassword, Duration ttl);

  String findByUserId(UUID userId);

  void delete(UUID userId);
}
