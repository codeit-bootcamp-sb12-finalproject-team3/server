package com.moduplaylist.api.user.service;

import com.moduplaylist.api.user.dto.UserCreateRequest;
import com.moduplaylist.api.user.dto.UserProfileResponse;
import com.moduplaylist.api.user.dto.UserProfileUpdateRequest;
import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.core.user.entity.UserRole;
import java.util.UUID;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

  CursorPageResponse<UserResponse> findAll(String emailLike, UserRole roleEqual, Boolean isLocked,
      String cursor, UUID idAfter, int limit, String sortBy, SortDirection sortDirection);

  UserResponse create(UserCreateRequest request);

  UserResponse updateRole(UUID userId, UserRole role);

  UserResponse updateLocked(UUID userId, boolean locked);

  UserProfileResponse getProfile(UUID userId);

  UserProfileResponse updateProfile(
      UUID userID,
      UUID authenticateUserId,
      UserProfileUpdateRequest request,
      MultipartFile image
  );
}
