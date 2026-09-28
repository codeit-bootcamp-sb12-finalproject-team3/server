
package com.moduplaylist.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordChangeRequest {

  @NotBlank
  private String currentPassword;

  @NotBlank
  private String newPassword;

  @NotBlank
  private String newPasswordConfirm;
}
