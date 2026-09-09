package com.moduplaylist.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserCreateRequest {

  @NotBlank
  @Size(max = 100)
  private String name;

  @NotBlank
  @Email
  @Size(max = 100)
  private String email;

  @NotBlank
  @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
  @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^\\p{L}\\p{N}\\s]).*$",
      message = "비밀번호에는 영문, 숫자, 특수문자가 각각 하나 이상 포함되어야 합니다."
  )
  private String password;

}
