package com.moduplaylist.api.recommendation.event;

import java.util.UUID;

public record PreferenceChangedEvent(
        UUID eventId,
        UUID userId,
        double appliedDelta
) {
}
