package com.moduplaylist.api.user.service;

import com.moduplaylist.api.user.dto.UserCreateRequest;
import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.core.user.entity.UserRole;
import java.util.UUID;

public interface UserService {

  UserResponse create(UserCreateRequest request);

  UserResponse updateRole(UUID userId, UserRole role);

  UserResponse updateLocked(UUID userId, boolean locked);
}
