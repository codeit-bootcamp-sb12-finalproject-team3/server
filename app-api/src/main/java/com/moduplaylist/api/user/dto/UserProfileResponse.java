package com.moduplaylist.api.user.dto;

import com.moduplaylist.core.user.entity.User;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {

  private UUID id;
  private String name;
  private String profileImageUrl;

  public static UserProfileResponse from(User user) {
    return UserProfileResponse.builder()
        .id(user.getId())
        .name(user.getName())
        .profileImageUrl(user.getProfileImageUrl())
        .build();
  }
}
