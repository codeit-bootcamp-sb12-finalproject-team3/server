package com.moduplaylist.api.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserProfileUpdateRequest {

  @Size(max = 50)
  @Pattern(regexp = ".*\\S.*", message = "name은 공백일 수 없습니다.")
  private String name;

  @Size(max = 500)
  private String profileImageUrl;
}
