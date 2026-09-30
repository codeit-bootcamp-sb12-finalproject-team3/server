package com.moduplaylist.core.user.repository;

import java.time.Duration;

public interface OAuth2LoginCodeStore {

  void save(String code, String browserToken, String userId, Duration ttl);

  String consume(String code, String browserToken);
}
