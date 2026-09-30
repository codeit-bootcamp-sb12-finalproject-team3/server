package com.moduplaylist.api.user.dto;

import com.moduplaylist.core.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserRoleUpdateRequest {

  @NotNull
  private UserRole role;
}