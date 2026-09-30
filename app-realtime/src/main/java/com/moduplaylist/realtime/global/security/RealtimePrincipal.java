package com.moduplaylist.realtime.global.security;

import java.security.Principal;
import java.util.UUID;

public record RealtimePrincipal(UUID userId) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}
