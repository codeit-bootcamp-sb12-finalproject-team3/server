package com.moduplaylist.api.auth.service;

import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.core.user.entity.OAuthProvider;
import com.moduplaylist.core.user.exception.InvalidOAuthAccountException;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuth2UserService {

  private final OAuth2UserProvisioningService provisioningService;

  public UserResponse findOrCreate(String registrationId, OAuth2User principal) {
    if (registrationId == null || principal == null) {
      throw new InvalidOAuthAccountException();
    }

    OAuthProvider provider = switch (registrationId.toLowerCase(Locale.ROOT)) {
      case "google" -> OAuthProvider.GOOGLE;
      case "kakao" -> OAuthProvider.KAKAO;
      default -> throw new InvalidOAuthAccountException();
    };

    return switch (provider) {
      case GOOGLE -> resolveGoogle(principal);
      case KAKAO -> resolveKakao(principal);
    };
  }

  private UserResponse resolveGoogle(OAuth2User principal) {
    String providerUserId = stringAttribute(principal.getAttribute("sub"));
    String email = stringAttribute(principal.getAttribute("email"));
    String name = stringAttribute(principal.getAttribute("name"));
    String profileImageUrl = stringAttribute(principal.getAttribute("picture"));

    if (providerUserId == null || email == null) {
      throw new InvalidOAuthAccountException();
    }

    if (name == null) {
      name = "Google User";
    }

    return provisioningService.findOrCreate(
        OAuthProvider.GOOGLE,
        providerUserId,
        email,
        name,
        profileImageUrl
    );
  }

  private UserResponse resolveKakao(OAuth2User principal) {
    String providerUserId = stringAttribute(principal.getAttribute("id"));

    if (providerUserId == null) {
      throw new InvalidOAuthAccountException();
    }

    Map<String, Object> properties = principal.getAttribute("properties");

    String name = properties == null
        ? null
        : stringAttribute(properties.get("nickname"));

    Map<String, Object> kakaoAccount = principal.getAttribute("kakao_account");

    Map<String, Object> profile = kakaoAccount == null
        ? null
        : (Map<String, Object>) kakaoAccount.get("profile");

    String profileImageUrl = profile == null
        ? null
        : stringAttribute(profile.get("profile_image_url"));

    if (name == null) {
      name = "Kakao User";
    }

    return provisioningService.findOrCreate(
        OAuthProvider.KAKAO,
        providerUserId,
        null,
        name,
        profileImageUrl
    );
  }

  private String stringAttribute(Object value) {
    if (value == null) {
      return null;
    }

    String result = String.valueOf(value).trim();
    return result.isBlank() ? null : result;
  }
}
