package com.moduplaylist.api.user.service;

import com.moduplaylist.api.user.dto.TokenRefreshResult;

public interface AuthService {

  TokenRefreshResult refresh(String refreshToken);

  void logout(String refreshToken);

}
