package com.moduplaylist.api.user.dto;

import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.entity.UserRole;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

  private UUID id;
  private Instant createdAt;
  private String email;
  private String name;
  private String profileImageUrl;
  private UserRole role;
  private boolean locked;

  public static UserResponse from(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .createdAt(user.getCreatedAt())
        .email(user.getEmail())
        .name(user.getName())
        .profileImageUrl(user.getProfileImageUrl())
        .role(user.getRole())
        .locked(user.isLocked())
        .build();
  }
}
