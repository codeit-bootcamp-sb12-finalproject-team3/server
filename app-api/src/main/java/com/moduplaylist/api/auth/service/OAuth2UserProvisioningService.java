package com.moduplaylist.api.auth.service;

import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.core.user.entity.OAuthProvider;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.entity.UserOAuthAccount;
import com.moduplaylist.core.user.exception.InvalidOAuthAccountException;
import com.moduplaylist.core.user.exception.UserAlreadyExistsException;
import com.moduplaylist.core.user.repository.UserOAuthAccountRepository;
import com.moduplaylist.core.user.repository.UserRepository;
import java.security.SecureRandom;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuth2UserProvisioningService {

  private static final int MAX_EMAIL_ATTEMPTS = 10;

  private final UserRepository userRepository;
  private final UserOAuthAccountRepository oAuthAccountRepository;

  private final SecureRandom secureRandom = new SecureRandom();

  @Transactional
  public UserResponse findOrCreate(
      OAuthProvider provider,
      String providerUserId,
      String email,
      String name,
      String profileImageUrl
  ) {
    if (provider == null || providerUserId == null || providerUserId.isBlank()) {
      throw new InvalidOAuthAccountException();
    }

    return oAuthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
        .map(account -> UserResponse.from(account.getUser()))
        .orElseGet(() -> createUser(provider, providerUserId, email, name, profileImageUrl));
  }

  private UserResponse createUser(
      OAuthProvider provider,
      String providerUserId,
      String email,
      String name,
      String profileImageUrl
  ) {
    if (name == null || name.isBlank()) {
      throw new InvalidOAuthAccountException();
    }

    String userEmail = switch (provider) {
      case GOOGLE -> resolveGoogleEmail(email);
      case KAKAO -> generateKakaoEmail(name);
    };

    User user = User.create(userEmail, null, name);

    if (profileImageUrl != null && !profileImageUrl.isBlank()) {
      user.updateProfile(null, profileImageUrl);
    }

    User savedUser = userRepository.save(user);

    oAuthAccountRepository.save(
        UserOAuthAccount.create(savedUser, provider, providerUserId)
    );

    return UserResponse.from(savedUser);
  }

  private String resolveGoogleEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new InvalidOAuthAccountException();
    }

    String normalizedEmail = email.trim();

    if (normalizedEmail.length() > 100) {
      throw new InvalidOAuthAccountException();
    }

    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new UserAlreadyExistsException();
    }

    return normalizedEmail;
  }

  private String generateKakaoEmail(String name) {
    String nickname = name.toLowerCase()
        .replaceAll("[^a-z0-9]", "");

    if (nickname.isBlank()) {
      nickname = "kakao";
    }

    if (nickname.length() > 70) {
      nickname = nickname.substring(0, 70);
    }

    for (int attempt = 0; attempt < MAX_EMAIL_ATTEMPTS; attempt++) {
      byte[] bytes = new byte[4];
      secureRandom.nextBytes(bytes);

      String random = HexFormat.of().formatHex(bytes);
      String email = nickname + "_" + random + "@kakao.com";

      if (!userRepository.existsByEmail(email)) {
        return email;
      }
    }

    throw new UserAlreadyExistsException();
  }
}
