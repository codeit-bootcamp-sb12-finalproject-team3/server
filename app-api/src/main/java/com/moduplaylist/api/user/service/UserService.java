package com.moduplaylist.api.user.service;

import com.moduplaylist.api.user.dto.UserCreateRequest;
import com.moduplaylist.api.user.dto.UserResponse;

public interface UserService {

  UserResponse create(UserCreateRequest request);
}
