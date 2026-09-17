package com.moduplaylist.realtime.global.security;

import java.util.UUID;

public record VerifiedAccessToken(UUID userId, String tokenId) {
}
