package com.moduplaylist.realtime.global.security;

import java.util.UUID;

/**
 * Read-only authentication session contract needed by realtime transports.
 */
public interface AccessTokenSessionRegistry {

    boolean isAccessTokenActive(UUID userId, String accessTokenId);
}
