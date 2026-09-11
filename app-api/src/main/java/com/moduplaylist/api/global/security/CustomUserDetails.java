package com.moduplaylist.api.global.security;

import com.moduplaylist.api.user.dto.UserResponse;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

  private final UserResponse userResponse;
  private final String password;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(
        new SimpleGrantedAuthority("ROLE_" + userResponse.getRole().name())
    );
  }

  @Override
  public String getUsername() {
    return userResponse.getEmail();
  }

  @Override
  public boolean isAccountNonLocked() {
    return !userResponse.isLocked();
  }

  public UUID getUserId() {
    return userResponse.getId();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    CustomUserDetails other = (CustomUserDetails) obj;

    return Objects.equals(
        userResponse.getId(),
        other.userResponse.getId()
    );
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(userResponse.getId());
  }

}
