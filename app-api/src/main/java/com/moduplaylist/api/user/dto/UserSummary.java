package com.moduplaylist.api.user.dto;

import com.moduplaylist.core.user.entity.User;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSummary {

  private UUID userId;
  private String name;
  private String profileImageUrl;

  public static UserSummary from(User user) {
    return UserSummary.builder()
        .userId(user.getId())
        .name(user.getName())
        .profileImageUrl(user.getProfileImageUrl())
        .build();
  }

}
