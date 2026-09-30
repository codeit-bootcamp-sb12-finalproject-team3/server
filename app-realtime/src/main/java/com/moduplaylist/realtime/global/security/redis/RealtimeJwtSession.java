package com.moduplaylist.realtime.global.security.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Minimal read model for the JSON stored at auth:jwt:{userId}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record RealtimeJwtSession(String accessTokenId) {
}
