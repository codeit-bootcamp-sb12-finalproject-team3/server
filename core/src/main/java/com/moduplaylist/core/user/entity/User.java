package com.moduplaylist.core.user.entity;

import com.moduplaylist.core.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

  @Column(name = "email", nullable = false, unique = true, length = 100)
  private String email;

  @Column(name = "password", length = 100)
  private String password;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "profile_image_url", length = 500)
  private String profileImageUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private UserRole role;

  @Column(name = "locked", nullable = false)
  private boolean locked;

  private User(String email, String password, String name) {
    this.email = email;
    this.password = password;
    this.name = name;
    this.role = UserRole.USER;
    this.locked = false;
  }

  public static User create(String email, String encodedPassword, String name) {
    return new User(email, encodedPassword, name);
  }
}
