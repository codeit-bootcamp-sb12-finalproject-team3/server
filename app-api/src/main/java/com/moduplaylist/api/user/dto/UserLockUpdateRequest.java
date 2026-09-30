package com.moduplaylist.api.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserLockUpdateRequest {

  @NotNull
  private Boolean locked;
}