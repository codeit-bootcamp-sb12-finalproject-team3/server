package com.moduplaylist.api.recommendation.service;

import com.moduplaylist.core.user.entity.User;
import java.util.Collection;
import java.util.UUID;

public interface UserContentGenrePreferenceService {

    void createFromInitialPreferences(User user, Collection<UUID> contentIds);
}
