package com.moduplaylist.core.user.repository;

import java.time.Duration;

public interface OAuth2AuthorizationRequestStore {

  void save(String state, String browserToken, String serializedRequest, Duration ttl);

  String find(String state, String browserToken);

  String consume(String state, String browserToken);
}
