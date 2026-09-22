package com.moduplaylist.api.auth.service;

import com.moduplaylist.api.auth.dto.TokenRefreshResult;

public interface AuthService {

  TokenRefreshResult refresh(String refreshToken);

  void logout(String refreshToken);

}
