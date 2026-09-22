package com.moduplaylist.api.recommendation.service;

import com.moduplaylist.core.activity.enums.PlaylistActivityType;
import java.util.UUID;

public interface PlaylistPreferenceUpdateService {

    void applyActivity(
            UUID userId,
            UUID playlistId,
            PlaylistActivityType activityType
    );
}
