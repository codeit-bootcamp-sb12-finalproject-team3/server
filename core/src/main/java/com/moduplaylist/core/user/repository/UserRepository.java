package com.moduplaylist.core.user.repository;

import com.moduplaylist.core.user.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);
}
