package com.moduplaylist.api.watchparty.event;

import java.util.UUID;

public record WatchPartyEndedEvent(UUID eventId, UUID partyId) {}

