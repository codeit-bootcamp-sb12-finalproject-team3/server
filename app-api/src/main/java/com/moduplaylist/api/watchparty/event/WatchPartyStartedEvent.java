package com.moduplaylist.api.watchparty.event;

import com.moduplaylist.core.watchparty.repository.WatchPartyPlaybackState;

import java.util.UUID;

public record WatchPartyStartedEvent(UUID eventId, UUID partyId, WatchPartyPlaybackState playbackState) {}
