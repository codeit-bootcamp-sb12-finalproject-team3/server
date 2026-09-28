package com.moduplaylist.core.user.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class InvalidOAuthAccountException extends BaseException {

  public InvalidOAuthAccountException() {
    super(ErrorCode.INVALID_OAUTH_ACCOUNT);
  }
}
