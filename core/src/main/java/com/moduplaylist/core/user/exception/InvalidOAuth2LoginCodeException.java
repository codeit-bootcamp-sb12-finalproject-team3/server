package com.moduplaylist.core.user.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class InvalidOAuth2LoginCodeException extends BaseException {

  public InvalidOAuth2LoginCodeException() {
    super(ErrorCode.INVALID_OAUTH2_LOGIN_CODE);
  }
}