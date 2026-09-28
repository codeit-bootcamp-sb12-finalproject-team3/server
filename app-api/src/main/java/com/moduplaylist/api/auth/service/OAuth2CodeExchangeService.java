package com.moduplaylist.api.auth.service;

import com.moduplaylist.api.auth.dto.TokenRefreshResult;
import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.InvalidOAuth2LoginCodeException;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.OAuth2LoginCodeStore;
import com.moduplaylist.core.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuth2CodeExchangeService {

  private final OAuth2LoginCodeStore loginCodeStore;
  private final UserRepository userRepository;
  private final LoginTokenIssuer loginTokenIssuer;

  @Transactional(readOnly = true)
  public TokenRefreshResult exchange(String code, String browserToken) {
    if (code == null || code.isBlank() || browserToken == null || browserToken.isBlank()) {
      throw new InvalidOAuth2LoginCodeException();
    }

    String userId = loginCodeStore.consume(code, browserToken);

    if (userId == null) {
      throw new InvalidOAuth2LoginCodeException();
    }

    UUID id = UUID.fromString(userId);

    User user = userRepository.findById(id)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    if (user.isLocked()) {
      throw new BaseException(ErrorCode.USER_LOCKED);
    }

    return loginTokenIssuer.issue(UserResponse.from(user));
  }
}
