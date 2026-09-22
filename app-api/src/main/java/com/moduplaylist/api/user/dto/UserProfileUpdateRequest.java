package com.moduplaylist.api.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserProfileUpdateRequest {

  @Size(max = 50)
  private String name;

  private String profileImageUrl;
}
